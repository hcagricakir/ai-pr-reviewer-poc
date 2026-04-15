package dev.prreviewer.ai;

import dev.prreviewer.config.ApplicationConfig;

public final class AiProviderFactory {

    public AiReviewProvider create(String providerName, ApplicationConfig applicationConfig) {
        return switch (providerName.toLowerCase()) {
            case "openai" -> new OpenAiReviewProvider(applicationConfig.providers().openai());
            case "mock" -> new MockReviewProvider();
            default -> throw new IllegalArgumentException("Unsupported provider: " + providerName);
        };
    }
}
