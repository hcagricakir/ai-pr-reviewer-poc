package dev.prreviewer.policy;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyLoaderTest {

    @Test
    void shouldMergeBaseAndProfilePoliciesInConfiguredOrder() {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        List<Path> policyPaths = List.of(
                projectRoot.resolve("policies/base/review-principles.yml"),
                projectRoot.resolve("policies/base/clean-code-rules.yml"),
                projectRoot.resolve("policies/base/severity-map.yml"),
                projectRoot.resolve("policies/base/output-contract.yml"),
                projectRoot.resolve("policies/profiles/java-backend.yml")
        );

        ResolvedPolicySet policySet = new PolicyLoader().load(policyPaths);

        assertThat(policySet.policyIds()).contains(
                "review-principles",
                "clean-code-rules",
                "severity-map",
                "output-contract",
                "java-backend-profile"
        );
        assertThat(policySet.checks()).extracting(PolicyDocument.PolicyItem::id)
                .contains("null-and-edge-handling", "optional-usage");
        assertThat(policySet.severityMapping()).containsEntry("high", "Likely bug, regression, or materially unsafe behavior. Should be fixed before merge.");
        assertThat(policySet.outputContract().allowedReviewActions()).containsExactly("comment", "request_changes");
        assertThat(policySet.outputContract().requiredFields()).contains("severity", "title", "recommendation");
    }
}
