package dev.prreviewer.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NormalizedReviewPayload(
        String summary,
        String overallAssessment,
        List<ReviewFinding> findings,
        List<String> notes
) {

    @JsonCreator
    public NormalizedReviewPayload(
            @JsonProperty("summary") String summary,
            @JsonProperty("overallAssessment") String overallAssessment,
            @JsonProperty("findings") List<ReviewFinding> findings,
            @JsonProperty("notes") List<String> notes
    ) {
        this.summary = summary == null ? "" : summary;
        this.overallAssessment = overallAssessment == null ? "no_findings" : overallAssessment;
        this.findings = findings == null ? List.of() : List.copyOf(findings);
        this.notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
