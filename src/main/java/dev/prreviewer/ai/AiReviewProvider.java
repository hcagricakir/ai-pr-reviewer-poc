package dev.prreviewer.ai;

import dev.prreviewer.review.NormalizedReviewPayload;
import dev.prreviewer.review.ReviewContext;

public interface AiReviewProvider {

    String name();

    NormalizedReviewPayload review(ReviewContext context);
}
