package dev.prreviewer.review;

import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.policy.PolicyDocument;
import dev.prreviewer.policy.ResolvedPolicySet;
import dev.prreviewer.util.ResourceLoader;
import dev.prreviewer.util.TextInterpolator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PromptTemplateRenderer {

    private final Path projectRoot;

    public PromptTemplateRenderer(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public String renderSystemPrompt(AgentProfile profile, ResolvedPolicySet policySet) {
        String template = readTemplate(profile.review().promptTemplate());
        Map<String, String> values = new LinkedHashMap<>();
        values.put("AGENT_NAME", profile.metadata().name());
        values.put("FOCUS_AREAS", renderFocusAreas(profile.review().focusAreas()));
        values.put("POLICY_SUMMARY", renderPolicySummary(policySet));
        values.put("OUTPUT_CONTRACT", renderOutputContract(policySet.outputContract()));
        return TextInterpolator.interpolateTemplate(template, values).trim();
    }

    public String renderSystemPrompt(AgentProfile profile, ResolvedPolicySet policySet, Path extraRulesFile) {
        String systemPrompt = renderSystemPrompt(profile, policySet);
        String extraRules = readTemplate(extraRulesFile.toString()).trim();
        if (extraRules.isBlank()) {
            return systemPrompt;
        }
        return systemPrompt
                + "\n\nSupplemental review rules:\n"
                + extraRules;
    }

    public String renderUserPrompt(dev.prreviewer.diff.ReviewInput reviewInput) {
        StringBuilder builder = new StringBuilder();
        builder.append("Review source: ").append(reviewInput.sourceType()).append('\n');
        builder.append("Review identifier: ").append(reviewInput.sourceId()).append('\n');
        builder.append("Title: ").append(reviewInput.title()).append('\n');
        builder.append('\n');
        builder.append("Changed files:\n");
        for (dev.prreviewer.diff.DiffSection section : reviewInput.diffSections()) {
            builder.append("- ").append(section.changedFile().path())
                    .append(" [").append(section.changedFile().changeType().name().toLowerCase()).append("]\n");
        }
        builder.append('\n');
        builder.append("Unified diff:\n");
        builder.append("```diff\n");
        builder.append(reviewInput.combinedDiff());
        builder.append("\n```");
        return builder.toString();
    }

    private String readTemplate(String location) {
        if (location.startsWith("classpath:")) {
            return ResourceLoader.readClasspathResource(location.substring("classpath:".length()));
        }
        Path path = projectRoot.resolve(location).normalize();
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read prompt template: " + path, exception);
        }
    }

    private String renderFocusAreas(List<String> focusAreas) {
        if (focusAreas.isEmpty()) {
            return "- correctness\n- maintainability";
        }
        return focusAreas.stream()
                .map(area -> "- " + area)
                .reduce((left, right) -> left + '\n' + right)
                .orElse("");
    }

    private String renderPolicySummary(ResolvedPolicySet policySet) {
        List<String> lines = new ArrayList<>();
        lines.add("Policies: " + String.join(", ", policySet.policyIds()));
        lines.add("Principles:");
        for (PolicyDocument.PolicyItem principle : policySet.principles()) {
            lines.add("- " + principle.title() + ": " + principle.guidance());
        }
        lines.add("Checks:");
        for (PolicyDocument.PolicyItem check : policySet.checks()) {
            String suffix = check.severityHint().isBlank() ? "" : " [severity hint: " + check.severityHint() + "]";
            lines.add("- " + check.title() + suffix + ": " + check.guidance());
        }
        if (!policySet.domainOverrides().isEmpty()) {
            lines.add("Domain overrides:");
            policySet.domainOverrides().forEach((key, values) -> {
                lines.add("- " + key + ":");
                values.forEach(value -> lines.add("  * " + value));
            });
        }
        return String.join("\n", lines);
    }

    private String renderOutputContract(PolicyDocument.OutputContract contract) {
        return String.join(
                "\n",
                List.of(
                        "- Summary style: " + contract.summaryStyle(),
                        "- No finding behavior: " + contract.noFindingBehavior(),
                        "- Confidence scale: " + contract.confidenceScale(),
                        "- Required fields: " + String.join(", ", contract.requiredFields()),
                        "- Line references: " + contract.lineReferenceGuidance(),
                        "- Allowed review actions: " + String.join(", ", contract.allowedReviewActions()),
                        "- Review action guidance: " + contract.reviewActionGuidance()
                )
        );
    }
}
