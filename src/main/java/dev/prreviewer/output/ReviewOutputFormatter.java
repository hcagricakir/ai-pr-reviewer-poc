package dev.prreviewer.output;

import dev.prreviewer.review.ReviewReport;

public interface ReviewOutputFormatter {

    String format(ReviewReport reviewReport);
}
