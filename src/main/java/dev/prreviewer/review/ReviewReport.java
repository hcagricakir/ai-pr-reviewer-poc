package dev.prreviewer.review;

import java.time.Instant;
import java.util.List;

public record ReviewReport(
        String reviewId,
        Instant createdAt,
        String provider,
        String source,
        String summary,
        String overallAssessment,
        ReviewAction reviewAction,
        List<ReviewFinding> findings,
        List<String> notes,
        List<String> appliedPolicies
) {
    public ReviewReport {
        reviewAction = reviewAction == null ? ReviewAction.COMMENT : reviewAction;
        findings = findings == null ? List.of() : List.copyOf(findings);
        notes = notes == null ? List.of() : List.copyOf(notes);
        appliedPolicies = appliedPolicies == null ? List.of() : List.copyOf(appliedPolicies);
    }
}
