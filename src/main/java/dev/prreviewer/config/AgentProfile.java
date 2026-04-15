package dev.prreviewer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AgentProfile(
        Metadata metadata,
        ReviewProfile review,
        OutputProfile output
) {

    @JsonCreator
    public AgentProfile(
            @JsonProperty("metadata") Metadata metadata,
            @JsonProperty("review") ReviewProfile review,
            @JsonProperty("output") OutputProfile output
    ) {
        this.metadata = metadata == null ? Metadata.defaults() : metadata;
        this.review = review == null ? ReviewProfile.defaults() : review;
        this.output = output == null ? OutputProfile.defaults() : output;
    }

    public record Metadata(String id, String name, String description, List<String> tags) {
        @JsonCreator
        public Metadata(
                @JsonProperty("id") String id,
                @JsonProperty("name") String name,
                @JsonProperty("description") String description,
                @JsonProperty("tags") List<String> tags
        ) {
            this.id = id == null ? "default-reviewer" : id;
            this.name = name == null ? "Default Reviewer" : name;
            this.description = description == null ? "" : description;
            this.tags = tags == null ? List.of() : List.copyOf(tags);
        }

        public static Metadata defaults() {
            return new Metadata(null, null, null, null);
        }
    }

    public record ReviewProfile(
            String promptTemplate,
            List<String> activePolicies,
            List<String> focusAreas,
            Boolean onlyActionableFindings,
            Integer maxFindings,
            Double minimumConfidence
    ) {
        @JsonCreator
        public ReviewProfile(
                @JsonProperty("promptTemplate") String promptTemplate,
                @JsonProperty("activePolicies") List<String> activePolicies,
                @JsonProperty("focusAreas") List<String> focusAreas,
                @JsonProperty("onlyActionableFindings") Boolean onlyActionableFindings,
                @JsonProperty("maxFindings") Integer maxFindings,
                @JsonProperty("minimumConfidence") Double minimumConfidence
        ) {
            this.promptTemplate = promptTemplate == null
                    ? "classpath:templates/reviewer-system-prompt.md"
                    : promptTemplate;
            this.activePolicies = activePolicies == null ? List.of() : List.copyOf(activePolicies);
            this.focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
            this.onlyActionableFindings = onlyActionableFindings == null || onlyActionableFindings;
            this.maxFindings = maxFindings == null ? 10 : maxFindings;
            this.minimumConfidence = minimumConfidence == null ? 0.55d : minimumConfidence;
        }

        public static ReviewProfile defaults() {
            return new ReviewProfile(null, null, null, null, null, null);
        }
    }

    public record OutputProfile(String formatter, Boolean includePolicySummary) {
        @JsonCreator
        public OutputProfile(
                @JsonProperty("formatter") String formatter,
                @JsonProperty("includePolicySummary") Boolean includePolicySummary
        ) {
            this.formatter = formatter == null ? "markdown" : formatter;
            this.includePolicySummary = includePolicySummary == null || includePolicySummary;
        }

        public static OutputProfile defaults() {
            return new OutputProfile(null, null);
        }
    }
}
