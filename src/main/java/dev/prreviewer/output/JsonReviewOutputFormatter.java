package dev.prreviewer.output;

import dev.prreviewer.review.ReviewReport;
import dev.prreviewer.util.ObjectMappers;

import java.io.IOException;

public final class JsonReviewOutputFormatter implements ReviewOutputFormatter {

    @Override
    public String format(ReviewReport reviewReport) {
        try {
            return ObjectMappers.json().writerWithDefaultPrettyPrinter().writeValueAsString(reviewReport);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize review report.", exception);
        }
    }
}
