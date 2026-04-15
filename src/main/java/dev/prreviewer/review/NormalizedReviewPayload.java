package dev.prreviewer.review;

import java.util.List;

public record NormalizedReviewPayload(
        String summary,
        String overallAssessment,
        ReviewAction reviewAction,
        List<ReviewFinding> findings,
        List<String> notes
) {
    public NormalizedReviewPayload {
        summary = summary == null ? "" : summary;
        overallAssessment = overallAssessment == null ? "no_findings" : overallAssessment;
        reviewAction = reviewAction == null ? ReviewAction.COMMENT : reviewAction;
        findings = findings == null ? List.of() : List.copyOf(findings);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
