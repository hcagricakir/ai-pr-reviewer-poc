package dev.prreviewer.output;

import dev.prreviewer.review.ReviewFinding;
import dev.prreviewer.review.ReviewReport;

public final class MarkdownReviewOutputFormatter implements ReviewOutputFormatter {

    @Override
    public String format(ReviewReport reviewReport) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Review Report\n\n");
        builder.append("- Review id: ").append(reviewReport.reviewId()).append('\n');
        builder.append("- Provider: ").append(reviewReport.provider()).append('\n');
        builder.append("- Source: ").append(reviewReport.source()).append('\n');
        builder.append("- Overall assessment: ").append(reviewReport.overallAssessment()).append('\n');
        builder.append("- Review action: ").append(reviewReport.reviewAction().apiValue()).append('\n');
        builder.append("- Applied policies: ").append(String.join(", ", reviewReport.appliedPolicies())).append('\n');
        builder.append('\n');
        builder.append("## Summary\n\n");
        builder.append(reviewReport.summary()).append('\n');
        builder.append('\n');

        if (reviewReport.findings().isEmpty()) {
            builder.append("## Findings\n\nNo actionable findings.\n");
        } else {
            builder.append("## Findings\n\n");
            for (ReviewFinding finding : reviewReport.findings()) {
                builder.append("- [").append(finding.severity().apiValue()).append("] ").append(finding.title()).append('\n');
                builder.append("  - File: ").append(finding.filePath()).append('\n');
                builder.append("  - Line: ").append(renderLineReference(finding)).append('\n');
                builder.append("  - Problem: ").append(finding.problem()).append('\n');
                if (!finding.codeSuggestion().isBlank()) {
                    builder.append('\n');
                    builder.append("```suggestion\n");
                    builder.append(finding.codeSuggestion()).append('\n');
                    builder.append("```\n");
                }
            }
        }

        if (!reviewReport.notes().isEmpty()) {
            builder.append('\n');
            builder.append("## Notes\n\n");
            reviewReport.notes().forEach(note -> builder.append("- ").append(note).append('\n'));
        }
        return builder.toString();
    }

    private String renderLineReference(ReviewFinding finding) {
        if (finding.startLine() == null) {
            return "n/a";
        }
        if (finding.endLine() == null || finding.endLine().equals(finding.startLine())) {
            return Integer.toString(finding.startLine());
        }
        return finding.startLine() + "-" + finding.endLine();
    }
}
