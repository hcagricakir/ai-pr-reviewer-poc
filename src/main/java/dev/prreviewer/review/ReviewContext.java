package dev.prreviewer.review;

import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.diff.ReviewInput;
import dev.prreviewer.policy.ResolvedPolicySet;

public record ReviewContext(
        String systemPrompt,
        String userPrompt,
        ReviewInput reviewInput,
        AgentProfile agentProfile,
        ResolvedPolicySet policySet
) {
}
