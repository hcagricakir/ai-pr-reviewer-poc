package dev.prreviewer.review;

import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.config.ConfigLoader;
import dev.prreviewer.config.EnvironmentContext;
import dev.prreviewer.policy.PolicyLoader;
import dev.prreviewer.policy.ResolvedPolicySet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateRendererTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAppendSupplementalRulesToSystemPrompt() throws Exception {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        Path extraRulesFile = tempDir.resolve("combined-rules.md");
        Files.writeString(
                extraRulesFile,
                """
                ## Shared rules
                - Always consider API compatibility
                - Flag missing validation on public inputs
                """
        );

        AgentProfile agentProfile = new ConfigLoader(EnvironmentContext.load(projectRoot))
                .loadAgentProfile(projectRoot.resolve("config/agents/default-reviewer.yml"));
        ResolvedPolicySet policySet = new PolicyLoader().load(List.of(
                projectRoot.resolve("policies/base/review-principles.yml"),
                projectRoot.resolve("policies/base/clean-code-rules.yml"),
                projectRoot.resolve("policies/base/severity-map.yml"),
                projectRoot.resolve("policies/base/output-contract.yml"),
                projectRoot.resolve("policies/profiles/java-backend.yml")
        ));

        String systemPrompt = new PromptTemplateRenderer(projectRoot)
                .renderSystemPrompt(agentProfile, policySet, extraRulesFile);

        assertThat(systemPrompt)
                .contains("Supplemental review rules:")
                .contains("Always consider API compatibility")
                .contains("Flag missing validation on public inputs");
    }
}
