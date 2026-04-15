package dev.prreviewer.diff;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewInputLoaderTest {

    @Test
    void shouldLoadSampleScenarioAndRenderUnifiedDiff() {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        ReviewInputLoader loader = new ReviewInputLoader(projectRoot);

        ReviewInput input = loader.loadSample("null-edge-case", "demo/scenarios");

        assertThat(input.sourceType()).isEqualTo("scenario");
        assertThat(input.sourceId()).isEqualTo("null-edge-case");
        assertThat(input.diffSections()).hasSize(1);
        assertThat(input.combinedDiff()).contains("orElse(null)", "@@");
        assertThat(input.metadata()).containsEntry(
                "description",
                "Introduces nullable control flow after a repository lookup and dereferences the nullable path."
        );
    }
}
