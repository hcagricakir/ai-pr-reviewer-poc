package dev.prreviewer.diff;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewInputFilterTest {

    @Test
    void shouldKeepRootLevelFilesThatMatchRecursiveIncludePattern() {
        ReviewInput input = new ReviewInput(
                "diff",
                "sample",
                "Sample review",
                List.of(
                        diffSection("App.java"),
                        diffSection("docs/README.md")
                ),
                Map.of()
        );

        ReviewInput filtered = new ReviewInputFilter().apply(
                input,
                List.of("**/*.java"),
                List.of(),
                10,
                10_000
        );

        assertThat(filtered.diffSections())
                .extracting(section -> section.changedFile().path())
                .containsExactly("App.java");
    }

    @Test
    void shouldTrackFilesSkippedByPatternAndLimits() {
        ReviewInput input = new ReviewInput(
                "diff",
                "sample",
                "Sample review",
                List.of(
                        diffSection("src/main/java/App.java"),
                        diffSection("src/main/java/Other.java"),
                        diffSection("README.md")
                ),
                Map.of("origin", "test")
        );

        ReviewInput filtered = new ReviewInputFilter().apply(
                input,
                List.of("**/*.java"),
                List.of(),
                1,
                10_000
        );

        assertThat(filtered.diffSections())
                .extracting(section -> section.changedFile().path())
                .containsExactly("src/main/java/App.java");
        assertThat(filtered.metadata())
                .containsEntry("origin", "test")
                .containsEntry("skippedByPattern", "1")
                .containsEntry("skippedByLimits", "1");
    }

    private DiffSection diffSection(String path) {
        return new DiffSection(
                new ChangedFile(path, ChangeType.MODIFIED, 1, 1, true),
                """
                diff --git a/%s b/%s
                @@ -1 +1 @@
                -old
                +new
                """.formatted(path, path)
        );
    }
}
