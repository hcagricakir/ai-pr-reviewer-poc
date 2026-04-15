package dev.prreviewer.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.prreviewer.config.ApplicationConfig;
import dev.prreviewer.review.NormalizedReviewPayload;
import dev.prreviewer.review.ReviewContext;
import dev.prreviewer.util.ObjectMappers;
import dev.prreviewer.util.ResourceLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class OpenAiReviewProvider implements AiReviewProvider {

    private final ApplicationConfig.OpenAiConfig config;
    private final HttpClient httpClient;
    private final JsonNode schema;

    public OpenAiReviewProvider(ApplicationConfig.OpenAiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        try {
            this.schema = ObjectMappers.json().readTree(ResourceLoader.readClasspathResource("schemas/review-report.schema.json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load review-report schema.", exception);
        }
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public NormalizedReviewPayload review(ReviewContext context) {
        if (config.apiKey().isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is missing. Configure it in .env or application.yml.");
        }

        ObjectNode requestBody = ObjectMappers.json().createObjectNode();
        requestBody.put("model", config.model());
        requestBody.putObject("reasoning").put("effort", config.reasoningEffort());
        requestBody.put("instructions", context.systemPrompt());
        requestBody.put("input", context.userPrompt());
        requestBody.set("text", buildStructuredOutputNode());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl().replaceAll("/$", "") + "/v1/responses"))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .header("X-Client-Request-Id", UUID.randomUUID().toString())
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(requestBody)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenAI request failed with status " + response.statusCode() + ": " + response.body());
            }
            JsonNode responseNode = ObjectMappers.json().readTree(response.body());
            String outputText = extractOutputText(responseNode);
            if (outputText.isBlank()) {
                throw new IllegalStateException("OpenAI response did not contain structured output text.");
            }
            return ObjectMappers.json().readValue(outputText, NormalizedReviewPayload.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse OpenAI response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request was interrupted.", exception);
        }
    }

    private ObjectNode buildStructuredOutputNode() {
        ObjectNode text = ObjectMappers.json().createObjectNode();
        ObjectNode format = text.putObject("format");
        format.put("type", "json_schema");
        format.put("name", "review_report");
        format.put("description", "Normalized pull request review findings.");
        format.put("strict", true);
        format.set("schema", schema);
        return text;
    }

    private String extractOutputText(JsonNode responseNode) {
        String directOutputText = responseNode.path("output_text").asText("");
        if (!directOutputText.isBlank()) {
            return directOutputText;
        }

        ArrayNode outputs = responseNode.has("output") && responseNode.get("output").isArray()
                ? (ArrayNode) responseNode.get("output")
                : ObjectMappers.json().createArrayNode();
        StringBuilder builder = new StringBuilder();
        for (JsonNode outputItem : outputs) {
            JsonNode content = outputItem.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if (contentItem.has("text")) {
                    builder.append(contentItem.path("text").asText());
                }
            }
        }
        return builder.toString();
    }

    private String writeJson(JsonNode payload) {
        try {
            return ObjectMappers.json().writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize OpenAI request.", exception);
        }
    }
}
