package dev.prreviewer.policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PolicyDocument(
        Metadata metadata,
        List<PolicyItem> principles,
        List<PolicyItem> checks,
        Map<String, String> severityMapping,
        OutputContract outputContract,
        Map<String, List<String>> domainOverrides
) {

    @JsonCreator
    public PolicyDocument(
            @JsonProperty("metadata") Metadata metadata,
            @JsonProperty("principles") List<PolicyItem> principles,
            @JsonProperty("checks") List<PolicyItem> checks,
            @JsonProperty("severityMapping") Map<String, String> severityMapping,
            @JsonProperty("outputContract") OutputContract outputContract,
            @JsonProperty("domainOverrides") Map<String, List<String>> domainOverrides
    ) {
        this.metadata = metadata == null ? Metadata.defaults() : metadata;
        this.principles = principles == null ? List.of() : List.copyOf(principles);
        this.checks = checks == null ? List.of() : List.copyOf(checks);
        this.severityMapping = severityMapping == null ? Map.of() : Map.copyOf(severityMapping);
        this.outputContract = outputContract == null ? OutputContract.defaults() : outputContract;
        this.domainOverrides = domainOverrides == null ? Map.of() : copyOverrides(domainOverrides);
    }

    private static Map<String, List<String>> copyOverrides(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Map.copyOf(copy);
    }

    public record Metadata(String id, String description, Integer precedence) {
        @JsonCreator
        public Metadata(
                @JsonProperty("id") String id,
                @JsonProperty("description") String description,
                @JsonProperty("precedence") Integer precedence
        ) {
            this.id = id == null ? "policy" : id;
            this.description = description == null ? "" : description;
            this.precedence = precedence == null ? 100 : precedence;
        }

        public static Metadata defaults() {
            return new Metadata(null, null, null);
        }
    }

    public record PolicyItem(
            String id,
            String title,
            String guidance,
            String severityHint,
            List<String> examples
    ) {
        @JsonCreator
        public PolicyItem(
                @JsonProperty("id") String id,
                @JsonProperty("title") String title,
                @JsonProperty("guidance") String guidance,
                @JsonProperty("severityHint") String severityHint,
                @JsonProperty("examples") List<String> examples
        ) {
            this.id = id == null ? "" : id;
            this.title = title == null ? "" : title;
            this.guidance = guidance == null ? "" : guidance;
            this.severityHint = severityHint == null ? "" : severityHint;
            this.examples = examples == null ? List.of() : List.copyOf(examples);
        }
    }

    public record OutputContract(
            String summaryStyle,
            String noFindingBehavior,
            String confidenceScale,
            List<String> requiredFields,
            String lineReferenceGuidance,
            List<String> allowedReviewActions,
            String reviewActionGuidance
    ) {
        @JsonCreator
        public OutputContract(
                @JsonProperty("summaryStyle") String summaryStyle,
                @JsonProperty("noFindingBehavior") String noFindingBehavior,
                @JsonProperty("confidenceScale") String confidenceScale,
                @JsonProperty("requiredFields") List<String> requiredFields,
                @JsonProperty("lineReferenceGuidance") String lineReferenceGuidance,
                @JsonProperty("allowedReviewActions") List<String> allowedReviewActions,
                @JsonProperty("reviewActionGuidance") String reviewActionGuidance
        ) {
            this.summaryStyle = summaryStyle == null ? "concise" : summaryStyle;
            this.noFindingBehavior = noFindingBehavior == null ? "" : noFindingBehavior;
            this.confidenceScale = confidenceScale == null ? "0.0 to 1.0" : confidenceScale;
            this.requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
            this.lineReferenceGuidance = lineReferenceGuidance == null ? "" : lineReferenceGuidance;
            this.allowedReviewActions = allowedReviewActions == null ? List.of() : List.copyOf(allowedReviewActions);
            this.reviewActionGuidance = reviewActionGuidance == null ? "" : reviewActionGuidance;
        }

        public static OutputContract defaults() {
            return new OutputContract(null, null, null, null, null, null, null);
        }
    }
}
