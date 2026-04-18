package dev.prreviewer.review;

import dev.prreviewer.ai.AiReviewProvider;
import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.diff.ReviewInput;
import dev.prreviewer.policy.ResolvedPolicySet;

import java.nio.file.Path;
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
        return prepareContext(reviewInput, agentProfile, policySet, null);
    }

    public ReviewContext prepareContext(
            ReviewInput reviewInput,
            AgentProfile agentProfile,
            ResolvedPolicySet policySet,
            Path extraRulesFile
    ) {
        String systemPrompt = promptTemplateRenderer.renderSystemPrompt(agentProfile, policySet);
        if (extraRulesFile != null) {
            systemPrompt = promptTemplateRenderer.renderSystemPrompt(agentProfile, policySet, extraRulesFile);
        }
        String userPrompt = promptTemplateRenderer.renderUserPrompt(reviewInput);
        return new ReviewContext(systemPrompt, userPrompt, reviewInput, agentProfile, policySet);
    }

    public ReviewReport review(
            AiReviewProvider provider,
            ReviewInput reviewInput,
            AgentProfile agentProfile,
            ResolvedPolicySet policySet
    ) {
        return review(provider, reviewInput, agentProfile, policySet, null);
    }

    public ReviewReport review(
            AiReviewProvider provider,
            ReviewInput reviewInput,
            AgentProfile agentProfile,
            ResolvedPolicySet policySet,
            Path extraRulesFile
    ) {
        ReviewContext context = prepareContext(reviewInput, agentProfile, policySet, extraRulesFile);
        NormalizedReviewPayload payload = provider.review(context);
        List<ReviewFinding> findings = payload.findings().stream()
                .filter(finding -> finding.confidence() >= agentProfile.review().minimumConfidence())
                .sorted(Comparator.comparing((ReviewFinding finding) -> finding.severity().ordinal()))
                .limit(agentProfile.review().maxFindings())
                .toList();
        ReviewAction reviewAction = resolveReviewAction(payload, findings);

        return new ReviewReport(
                UUID.randomUUID().toString(),
                Instant.now(),
                provider.name(),
                reviewInput.sourceType() + ":" + reviewInput.sourceId(),
                payload.summary(),
                payload.overallAssessment(),
                reviewAction,
                findings,
                payload.notes(),
                policySet.policyIds()
        );
    }

    private ReviewAction resolveReviewAction(NormalizedReviewPayload payload, List<ReviewFinding> findings) {
        if (findings.isEmpty()) {
            return ReviewAction.COMMENT;
        }
        if ("blocked".equalsIgnoreCase(payload.overallAssessment())) {
            return ReviewAction.COMMENT;
        }
        return payload.reviewAction();
    }
}
