package dev.prreviewer.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.prreviewer.config.ApplicationConfig;
import dev.prreviewer.review.ReviewFinding;
import dev.prreviewer.review.ReviewReport;
import dev.prreviewer.util.ObjectMappers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GitHubReviewPublisher {

    static final String COMMENT_MARKER = "<!-- ai-pr-reviewer-poc -->";
    private static final String BOT_LOGIN = "github-actions[bot]";

    private final ApplicationConfig.GitHubConfig gitHubConfig;
    private final HttpClient httpClient;

    public GitHubReviewPublisher(ApplicationConfig.GitHubConfig gitHubConfig) {
        this.gitHubConfig = gitHubConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public PublishResult publishInlineComments(int pullRequestNumber, ReviewReport reviewReport) {
        validateGitHubConfig();
        PullRequestSnapshot pullRequestSnapshot = loadPullRequestSnapshot(pullRequestNumber);
        List<CommentDraft> drafts = buildCommentDrafts(reviewReport);
        if (drafts.isEmpty()) {
            return new PublishResult(0, reviewReport.findings().size(), List.of("No findings had enough file and line context for inline comments."));
        }

        Set<CommentFingerprint> existingFingerprints = loadExistingFingerprints(pullRequestNumber);
        int publishedComments = 0;
        int skippedFindings = 0;
        List<String> notes = new ArrayList<>();

        for (CommentDraft draft : drafts) {
            if (existingFingerprints.contains(draft.fingerprint(pullRequestSnapshot.headSha()))) {
                skippedFindings += draft.findings().size();
                notes.add("Skipped duplicate inline comment for %s:%d".formatted(draft.path(), draft.line()));
                continue;
            }
            createReviewComment(pullRequestNumber, pullRequestSnapshot.headSha(), draft);
            publishedComments++;
        }

        int totalDraftFindings = drafts.stream().mapToInt(draft -> draft.findings().size()).sum();
        skippedFindings += reviewReport.findings().size() - totalDraftFindings;
        return new PublishResult(publishedComments, skippedFindings, List.copyOf(new LinkedHashSet<>(notes)));
    }

    static List<CommentDraft> buildCommentDrafts(ReviewReport reviewReport) {
        Map<CommentAnchor, List<ReviewFinding>> groupedFindings = new LinkedHashMap<>();
        for (ReviewFinding finding : reviewReport.findings()) {
            if (finding.filePath().isBlank() || finding.startLine() == null) {
                continue;
            }
            int endLine = finding.endLine() != null && finding.endLine() >= finding.startLine()
                    ? finding.endLine()
                    : finding.startLine();
            CommentAnchor anchor = new CommentAnchor(finding.filePath(), finding.startLine(), endLine);
            groupedFindings.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(finding);
        }

        List<CommentDraft> drafts = new ArrayList<>();
        groupedFindings.forEach((anchor, findings) -> drafts.add(new CommentDraft(
                anchor.path(),
                anchor.startLine(),
                anchor.endLine(),
                renderCommentBody(findings),
                List.copyOf(findings)
        )));
        drafts.sort(Comparator.comparing(CommentDraft::path).thenComparing(CommentDraft::line));
        return List.copyOf(drafts);
    }

    static String renderCommentBody(List<ReviewFinding> findings) {
        StringBuilder builder = new StringBuilder();
        builder.append(COMMENT_MARKER).append("\n");

        if (findings.size() == 1) {
            ReviewFinding finding = findings.getFirst();
            builder.append("**[").append(finding.severity().apiValue()).append("] ").append(finding.title()).append("**\n\n");
            builder.append("Problem: ").append(finding.problem()).append("\n\n");
            builder.append("Why it matters: ").append(finding.whyItMatters()).append("\n\n");
            builder.append("Recommendation: ").append(finding.recommendation()).append("\n\n");
            builder.append("Confidence: ").append(String.format("%.2f", finding.confidence()));
            return builder.toString();
        }

        builder.append("Multiple actionable findings were detected on this code region.\n\n");
        for (int index = 0; index < findings.size(); index++) {
            ReviewFinding finding = findings.get(index);
            builder.append(index + 1).append(". **[").append(finding.severity().apiValue()).append("] ")
                    .append(finding.title()).append("**\n");
            builder.append("   Problem: ").append(finding.problem()).append("\n");
            builder.append("   Why it matters: ").append(finding.whyItMatters()).append("\n");
            builder.append("   Recommendation: ").append(finding.recommendation()).append("\n");
            builder.append("   Confidence: ").append(String.format("%.2f", finding.confidence())).append("\n");
        }
        return builder.toString().stripTrailing();
    }

    private PullRequestSnapshot loadPullRequestSnapshot(int pullRequestNumber) {
        JsonNode pullRequest = sendRequest(
                "GET",
                "/repos/%s/%s/pulls/%d".formatted(
                        urlEncode(gitHubConfig.owner()),
                        urlEncode(gitHubConfig.repo()),
                        pullRequestNumber
                ),
                null
        );
        String headSha = pullRequest.path("head").path("sha").asText("");
        if (headSha.isBlank()) {
            throw new IllegalStateException("GitHub pull request response did not contain head.sha for PR #" + pullRequestNumber);
        }
        return new PullRequestSnapshot(headSha);
    }

    private Set<CommentFingerprint> loadExistingFingerprints(int pullRequestNumber) {
        Set<CommentFingerprint> fingerprints = new LinkedHashSet<>();
        for (int page = 1; ; page++) {
            JsonNode comments = sendRequest(
                    "GET",
                    "/repos/%s/%s/pulls/%d/comments?per_page=100&page=%d".formatted(
                            urlEncode(gitHubConfig.owner()),
                            urlEncode(gitHubConfig.repo()),
                            pullRequestNumber,
                            page
                    ),
                    null
            );
            if (!comments.isArray() || comments.isEmpty()) {
                return fingerprints;
            }
            for (JsonNode comment : comments) {
                if (!BOT_LOGIN.equalsIgnoreCase(comment.path("user").path("login").asText(""))) {
                    continue;
                }
                String body = comment.path("body").asText("");
                if (!body.contains(COMMENT_MARKER)) {
                    continue;
                }
                String path = comment.path("path").asText("");
                int line = comment.path("line").asInt(0);
                int startLine = comment.path("start_line").asInt(line);
                String commitId = comment.path("commit_id").asText("");
                if (path.isBlank() || line <= 0 || commitId.isBlank()) {
                    continue;
                }
                fingerprints.add(new CommentFingerprint(path, startLine, line, commitId, normalizeBody(body)));
            }
            if (comments.size() < 100) {
                return fingerprints;
            }
        }
    }

    private void createReviewComment(int pullRequestNumber, String commitSha, CommentDraft draft) {
        ObjectNode payload = ObjectMappers.json().createObjectNode();
        payload.put("body", draft.body());
        payload.put("commit_id", commitSha);
        payload.put("path", draft.path());
        payload.put("line", draft.line());
        payload.put("side", "RIGHT");
        if (draft.startLine() < draft.line()) {
            payload.put("start_line", draft.startLine());
            payload.put("start_side", "RIGHT");
        }

        sendRequest(
                "POST",
                "/repos/%s/%s/pulls/%d/comments".formatted(
                        urlEncode(gitHubConfig.owner()),
                        urlEncode(gitHubConfig.repo()),
                        pullRequestNumber
                ),
                payload
        );
    }

    private JsonNode sendRequest(String method, String path, JsonNode payload) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(gitHubConfig.apiBaseUrl().replaceAll("/$", "") + path))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + gitHubConfig.token())
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.ofSeconds(30));

        try {
            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.header("Content-Type", "application/json");
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(ObjectMappers.json().writeValueAsString(payload)));
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GitHub API request failed with status " + response.statusCode() + ": " + response.body());
            }
            return ObjectMappers.json().readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub API request was interrupted for path " + path, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub API request failed for path " + path, exception);
        }
    }

    private void validateGitHubConfig() {
        if (gitHubConfig.owner().isBlank() || gitHubConfig.repo().isBlank() || gitHubConfig.token().isBlank()) {
            throw new IllegalArgumentException("GitHub owner, repo, and token must be configured to publish PR review comments.");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizeBody(String body) {
        return body.replace("\r\n", "\n").trim();
    }

    record CommentAnchor(String path, int startLine, int endLine) {
    }

    record PullRequestSnapshot(String headSha) {
    }

    record CommentDraft(String path, int startLine, int line, String body, List<ReviewFinding> findings) {
        CommentFingerprint fingerprint(String commitSha) {
            return new CommentFingerprint(path, startLine, line, commitSha, normalizeBody(body));
        }
    }

    record CommentFingerprint(String path, int startLine, int line, String commitSha, String body) {
    }

    public record PublishResult(int publishedComments, int skippedFindings, List<String> notes) {
        public PublishResult {
            notes = notes == null ? List.of() : List.copyOf(notes);
        }
    }
}
