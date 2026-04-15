package dev.prreviewer.github;

import dev.prreviewer.review.ReviewFinding;
import dev.prreviewer.review.ReviewReport;
import dev.prreviewer.review.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubReviewPublisherTest {

    @Test
    void shouldBuildOneDraftPerAnchorAndSkipFindingsWithoutLineContext() {
        ReviewFinding first = new ReviewFinding(
                Severity.HIGH,
                "Null guard regression",
                "Null can reach compareTo.",
                "This changes the method contract.",
                "Restore the null check.",
                0.99,
                "src/main/java/example/Foo.java",
                6,
                9
        );
        ReviewFinding second = new ReviewFinding(
                Severity.MEDIUM,
                "Naming issue",
                "Variable name is vague.",
                "It hurts readability.",
                "Rename the variable.",
                0.70,
                "src/main/java/example/Foo.java",
                6,
                9
        );
        ReviewFinding skipped = new ReviewFinding(
                Severity.LOW,
                "No anchor",
                "No line information.",
                "Cannot publish inline.",
                "Add line context.",
                0.60,
                "src/main/java/example/Foo.java",
                null,
                null
        );

        ReviewReport report = new ReviewReport(
                "review-1",
                Instant.now(),
                "mock",
                "github-pr:42",
                "summary",
                "concerns",
                List.of(first, second, skipped),
                List.of(),
                List.of("policy")
        );

        List<GitHubReviewPublisher.CommentDraft> drafts = GitHubReviewPublisher.buildCommentDrafts(report);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.getFirst().path()).isEqualTo("src/main/java/example/Foo.java");
        assertThat(drafts.getFirst().startLine()).isEqualTo(6);
        assertThat(drafts.getFirst().line()).isEqualTo(9);
        assertThat(drafts.getFirst().findings()).hasSize(2);
    }

    @Test
    void shouldRenderMarkerAndSingleFindingBody() {
        ReviewFinding finding = new ReviewFinding(
                Severity.HIGH,
                "Null guard regression",
                "Null can reach compareTo.",
                "This changes the method contract.",
                "Restore the null check.",
                0.99,
                "src/main/java/example/Foo.java",
                6,
                9
        );

        String body = GitHubReviewPublisher.renderCommentBody(List.of(finding));

        assertThat(body).contains(GitHubReviewPublisher.COMMENT_MARKER);
        assertThat(body).contains("**[high] Null guard regression**");
        assertThat(body).contains("Problem: Null can reach compareTo.");
        assertThat(body).contains("Confidence: 0.99");
    }
}
