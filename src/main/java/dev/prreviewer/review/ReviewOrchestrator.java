package dev.prreviewer.review;

import dev.prreviewer.ai.AiReviewProvider;
import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.diff.ReviewInput;
import dev.prreviewer.policy.ResolvedPolicySet;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ReviewOrchestrator {

    private final PromptTemplateRenderer promptTemplateRenderer;

    public ReviewOrchestrator(PromptTemplateRenderer promptTemplateRenderer) {
        this.promptTemplateRenderer = promptTemplateRenderer;
    }

    public ReviewContext prepareContext(ReviewInput reviewInput, AgentProfile agentProfile, ResolvedPolicySet policySet) {
        String systemPrompt = promptTemplateRenderer.renderSystemPrompt(agentProfile, policySet);
        String userPrompt = promptTemplateRenderer.renderUserPrompt(reviewInput);
        return new ReviewContext(systemPrompt, userPrompt, reviewInput, agentProfile, policySet);
    }

    public ReviewReport review(
            AiReviewProvider provider,
            ReviewInput reviewInput,
            AgentProfile agentProfile,
            ResolvedPolicySet policySet
    ) {
        ReviewContext context = prepareContext(reviewInput, agentProfile, policySet);
        NormalizedReviewPayload payload = provider.review(context);
        List<ReviewFinding> findings = payload.findings().stream()
                .filter(finding -> finding.confidence() >= agentProfile.review().minimumConfidence())
                .sorted(Comparator.comparing((ReviewFinding finding) -> finding.severity().ordinal()))
                .limit(agentProfile.review().maxFindings())
                .toList();

        return new ReviewReport(
                UUID.randomUUID().toString(),
                Instant.now(),
                provider.name(),
                reviewInput.sourceType() + ":" + reviewInput.sourceId(),
                payload.summary(),
                payload.overallAssessment(),
                findings,
                payload.notes(),
                policySet.policyIds()
        );
    }
}
