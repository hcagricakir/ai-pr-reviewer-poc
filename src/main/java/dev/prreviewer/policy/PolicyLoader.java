package dev.prreviewer.policy;

import dev.prreviewer.util.ObjectMappers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class PolicyLoader {

    public ResolvedPolicySet load(List<Path> paths) {
        List<PolicyDocument> documents = new ArrayList<>();
        for (Path path : paths) {
            documents.add(read(path));
        }
        documents.sort(Comparator.comparingInt(document -> document.metadata().precedence()));
        return merge(documents);
    }

    private PolicyDocument read(Path path) {
        try {
            return ObjectMappers.yaml().readValue(Files.readString(path), PolicyDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load policy file: " + path, exception);
        }
    }

    private ResolvedPolicySet merge(List<PolicyDocument> documents) {
        List<String> policyIds = new ArrayList<>();
        List<PolicyDocument.PolicyItem> principles = new ArrayList<>();
        List<PolicyDocument.PolicyItem> checks = new ArrayList<>();
        Map<String, String> severityMapping = new LinkedHashMap<>();
        Map<String, List<String>> domainOverrides = new LinkedHashMap<>();
        PolicyDocument.OutputContract outputContract = PolicyDocument.OutputContract.defaults();

        for (PolicyDocument document : documents) {
            policyIds.add(document.metadata().id());
            principles.addAll(document.principles());
            checks.addAll(document.checks());
            severityMapping.putAll(document.severityMapping());
            outputContract = mergeOutputContract(outputContract, document.outputContract());
            mergeOverrides(domainOverrides, document.domainOverrides());
        }

        return new ResolvedPolicySet(
                List.copyOf(new LinkedHashSet<>(policyIds)),
                List.copyOf(principles),
                List.copyOf(checks),
                Map.copyOf(severityMapping),
                outputContract,
                copyOverrides(domainOverrides)
        );
    }

    private void mergeOverrides(Map<String, List<String>> target, Map<String, List<String>> source) {
        source.forEach((key, value) -> {
            List<String> merged = new ArrayList<>(target.getOrDefault(key, List.of()));
            merged.addAll(value);
            target.put(key, List.copyOf(merged));
        });
    }

    private Map<String, List<String>> copyOverrides(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private PolicyDocument.OutputContract mergeOutputContract(
            PolicyDocument.OutputContract base,
            PolicyDocument.OutputContract override
    ) {
        String summaryStyle = override.summaryStyle().isBlank() ? base.summaryStyle() : override.summaryStyle();
        String noFindingBehavior = override.noFindingBehavior().isBlank()
                ? base.noFindingBehavior()
                : override.noFindingBehavior();
        String confidenceScale = override.confidenceScale().isBlank()
                ? base.confidenceScale()
                : override.confidenceScale();
        List<String> requiredFields = override.requiredFields().isEmpty()
                ? base.requiredFields()
                : override.requiredFields();
        String lineReferenceGuidance = override.lineReferenceGuidance().isBlank()
                ? base.lineReferenceGuidance()
                : override.lineReferenceGuidance();
        List<String> allowedReviewActions = override.allowedReviewActions().isEmpty()
                ? base.allowedReviewActions()
                : override.allowedReviewActions();
        String reviewActionGuidance = override.reviewActionGuidance().isBlank()
                ? base.reviewActionGuidance()
                : override.reviewActionGuidance();
        return new PolicyDocument.OutputContract(
                summaryStyle,
                noFindingBehavior,
                confidenceScale,
                requiredFields,
                lineReferenceGuidance,
                allowedReviewActions,
                reviewActionGuidance
        );
    }
}
