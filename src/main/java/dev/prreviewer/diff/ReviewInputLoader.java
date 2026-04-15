package dev.prreviewer.diff;

import com.fasterxml.jackson.databind.JsonNode;
import dev.prreviewer.config.ApplicationConfig;
import dev.prreviewer.util.ObjectMappers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReviewInputLoader {

    private final Path projectRoot;
    private final DiffGenerator diffGenerator;
    private final UnifiedDiffParser unifiedDiffParser;
    private final HttpClient httpClient;

    public ReviewInputLoader(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.diffGenerator = new DiffGenerator();
        this.unifiedDiffParser = new UnifiedDiffParser();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public ReviewInput loadFromDiffFile(Path diffFile) {
        try {
            String diffText = Files.readString(diffFile);
            List<DiffSection> sections = unifiedDiffParser.parse(diffText);
            return new ReviewInput(
                    "diff-file",
                    diffFile.toAbsolutePath().toString(),
                    "Diff file review",
                    sections,
                    Map.of()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read diff file: " + diffFile, exception);
        }
    }

    public ReviewInput loadSample(String sampleId, String scenariosDirectory) {
        Path manifestPath = projectRoot.resolve(scenariosDirectory).resolve(sampleId).resolve("scenario.yml");
        return loadFromScenarioManifest(manifestPath);
    }

    public ReviewInput loadFromScenarioManifest(Path manifestPath) {
        try {
            ScenarioManifest manifest = ObjectMappers.yaml().readValue(Files.readString(manifestPath), ScenarioManifest.class);
            List<DiffSection> sections = new ArrayList<>();
            for (ScenarioManifest.ScenarioChange change : manifest.changes()) {
                Path basePath = resolveIfPresent(manifestPath.getParent(), change.base());
                Path headPath = resolveIfPresent(manifestPath.getParent(), change.head());
                String baseContent = basePath == null ? "" : Files.readString(basePath);
                String headContent = headPath == null ? "" : Files.readString(headPath);
                ChangeType changeType = determineChangeType(change);
                sections.add(diffGenerator.generate(change.path(), changeType, baseContent, headContent));
            }
            Map<String, String> metadata = Map.of("description", manifest.scenario().description());
            return new ReviewInput(
                    "scenario",
                    manifest.scenario().id(),
                    manifest.scenario().title(),
                    sections,
                    metadata
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load scenario manifest: " + manifestPath, exception);
        }
    }

    public ReviewInput loadFromGitHubPullRequest(int pullRequestNumber, ApplicationConfig.GitHubConfig gitHubConfig) {
        validateGitHubConfig(gitHubConfig);
        String baseUrl = gitHubConfig.apiBaseUrl().replaceAll("/$", "");
        JsonNode pullRequest = getJson(
                "%s/repos/%s/%s/pulls/%d".formatted(
                        baseUrl,
                        urlEncode(gitHubConfig.owner()),
                        urlEncode(gitHubConfig.repo()),
                        pullRequestNumber
                ),
                gitHubConfig.token()
        );
        String title = pullRequest.path("title").asText("Pull request " + pullRequestNumber);

        List<DiffSection> sections = new ArrayList<>();
        List<String> omittedFiles = new ArrayList<>();

        for (int page = 1; ; page++) {
            JsonNode files = getJson(
                    "%s/repos/%s/%s/pulls/%d/files?per_page=100&page=%d".formatted(
                            baseUrl,
                            urlEncode(gitHubConfig.owner()),
                            urlEncode(gitHubConfig.repo()),
                            pullRequestNumber,
                            page
                    ),
                    gitHubConfig.token()
            );
            if (!files.isArray() || files.isEmpty()) {
                break;
            }
            for (JsonNode file : files) {
                String patch = file.path("patch").asText("");
                String filename = file.path("filename").asText("");
                if (patch.isBlank() || filename.isBlank()) {
                    omittedFiles.add(filename.isBlank() ? "<unknown>" : filename);
                    continue;
                }
                ChangeType changeType = parseGitHubChangeType(file.path("status").asText("modified"));
                sections.add(new DiffSection(
                        new ChangedFile(
                                filename,
                                changeType,
                                file.path("additions").asInt(0),
                                file.path("deletions").asInt(0),
                                true
                        ),
                        renderGitHubUnifiedDiff(file, patch)
                ));
            }
            if (files.size() < 100) {
                break;
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        if (!omittedFiles.isEmpty()) {
            metadata.put("omittedFiles", String.join(", ", omittedFiles));
        }
        metadata.put("repository", gitHubConfig.owner() + "/" + gitHubConfig.repo());
        return new ReviewInput(
                "github-pr",
                Integer.toString(pullRequestNumber),
                title,
                sections,
                metadata
        );
    }

    private JsonNode getJson(String url, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GitHub API request failed with status " + response.statusCode() + ": " + response.body());
            }
            return ObjectMappers.json().readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub API request was interrupted for " + url, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("GitHub API request failed for " + url, exception);
        }
    }

    private String renderGitHubUnifiedDiff(JsonNode file, String patch) {
        String filename = file.path("filename").asText();
        String previousFilename = file.path("previous_filename").asText(filename);
        ChangeType changeType = parseGitHubChangeType(file.path("status").asText("modified"));

        StringBuilder builder = new StringBuilder();
        builder.append("diff --git a/").append(previousFilename).append(" b/").append(filename).append('\n');
        if (changeType == ChangeType.RENAMED) {
            builder.append("rename from ").append(previousFilename).append('\n');
            builder.append("rename to ").append(filename).append('\n');
        }
        builder.append("--- ").append(changeType == ChangeType.ADDED ? "/dev/null" : "a/" + previousFilename).append('\n');
        builder.append("+++ ").append(changeType == ChangeType.REMOVED ? "/dev/null" : "b/" + filename).append('\n');
        builder.append(patch);
        return builder.toString();
    }

    private void validateGitHubConfig(ApplicationConfig.GitHubConfig gitHubConfig) {
        if (gitHubConfig.owner().isBlank() || gitHubConfig.repo().isBlank() || gitHubConfig.token().isBlank()) {
            throw new IllegalArgumentException("GitHub owner, repo, and token must be configured for PR review input.");
        }
    }

    private ChangeType determineChangeType(ScenarioManifest.ScenarioChange change) {
        if (!change.changeType().isBlank()) {
            return ChangeType.valueOf(change.changeType().trim().toUpperCase());
        }
        if (change.base().isBlank()) {
            return ChangeType.ADDED;
        }
        if (change.head().isBlank()) {
            return ChangeType.REMOVED;
        }
        return ChangeType.MODIFIED;
    }

    private ChangeType parseGitHubChangeType(String status) {
        return switch (status.toLowerCase()) {
            case "added" -> ChangeType.ADDED;
            case "removed" -> ChangeType.REMOVED;
            case "renamed" -> ChangeType.RENAMED;
            default -> ChangeType.MODIFIED;
        };
    }

    private Path resolveIfPresent(Path basePath, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return basePath.resolve(relativePath).normalize();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
