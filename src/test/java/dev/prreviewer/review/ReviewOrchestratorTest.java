package dev.prreviewer.review;

import dev.prreviewer.ai.MockReviewProvider;
import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.diff.ReviewInput;
import dev.prreviewer.diff.ReviewInputLoader;
import dev.prreviewer.policy.PolicyLoader;
import dev.prreviewer.policy.ResolvedPolicySet;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewOrchestratorTest {

    @Test
    void shouldReturnNoFindingsForCleanSample() {
        ReviewReport report = runSample("clean-implementation");

        assertThat(report.findings()).isEmpty();
        assertThat(report.overallAssessment()).isEqualTo("no_findings");
    }

    @Test
    void shouldFindPerformanceConcernForPerformanceSample() {
        ReviewReport report = runSample("performance-smell");

        assertThat(report.findings()).isNotEmpty();
        assertThat(report.findings()).extracting(ReviewFinding::title)
                .contains("Potential repeated expensive call inside loop");
    }

    private ReviewReport runSample(String sampleId) {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        ReviewInput input = new ReviewInputLoader(projectRoot).loadSample(sampleId, "demo/scenarios");
        AgentProfile agentProfile = new dev.prreviewer.config.ConfigLoader(dev.prreviewer.config.EnvironmentContext.load(projectRoot))
                .loadAgentProfile(projectRoot.resolve("config/agents/default-reviewer.yml"));
        ResolvedPolicySet policySet = new PolicyLoader().load(List.of(
                projectRoot.resolve("policies/base/review-principles.yml"),
                projectRoot.resolve("policies/base/clean-code-rules.yml"),
                projectRoot.resolve("policies/base/severity-map.yml"),
                projectRoot.resolve("policies/base/output-contract.yml"),
                projectRoot.resolve("policies/profiles/java-backend.yml")
        ));
        ReviewOrchestrator orchestrator = new ReviewOrchestrator(new PromptTemplateRenderer(projectRoot));
        return orchestrator.review(new MockReviewProvider(), input, agentProfile, policySet);
    }
}
