package dev.prreviewer.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ApplicationConfig(
        ReviewRuntimeConfig review,
        ProvidersConfig providers,
        GitHubConfig github,
        DemoConfig demo
) {

    @JsonCreator
    public ApplicationConfig(
            @JsonProperty("review") ReviewRuntimeConfig review,
            @JsonProperty("providers") ProvidersConfig providers,
            @JsonProperty("github") GitHubConfig github,
            @JsonProperty("demo") DemoConfig demo
    ) {
        this.review = review == null ? ReviewRuntimeConfig.defaults() : review;
        this.providers = providers == null ? ProvidersConfig.defaults() : providers;
        this.github = github == null ? GitHubConfig.defaults() : github;
        this.demo = demo == null ? DemoConfig.defaults() : demo;
    }

    public record ReviewRuntimeConfig(
            String provider,
            String agentProfile,
            String outputFormat,
            Integer maxFiles,
            Integer maxDiffCharacters,
            List<String> includePatterns,
            List<String> excludePatterns
    ) {
        @JsonCreator
        public ReviewRuntimeConfig(
                @JsonProperty("provider") String provider,
                @JsonProperty("agentProfile") String agentProfile,
                @JsonProperty("outputFormat") String outputFormat,
                @JsonProperty("maxFiles") Integer maxFiles,
                @JsonProperty("maxDiffCharacters") Integer maxDiffCharacters,
                @JsonProperty("includePatterns") List<String> includePatterns,
                @JsonProperty("excludePatterns") List<String> excludePatterns
        ) {
            this.provider = provider == null ? "mock" : provider;
            this.agentProfile = agentProfile == null ? "config/agents/default-reviewer.yml" : agentProfile;
            this.outputFormat = outputFormat == null ? "markdown" : outputFormat;
            this.maxFiles = maxFiles == null ? 25 : maxFiles;
            this.maxDiffCharacters = maxDiffCharacters == null ? 30000 : maxDiffCharacters;
            this.includePatterns = includePatterns == null ? List.of("**/*.java") : List.copyOf(includePatterns);
            this.excludePatterns = excludePatterns == null ? List.of() : List.copyOf(excludePatterns);
        }

        public static ReviewRuntimeConfig defaults() {
            return new ReviewRuntimeConfig(null, null, null, null, null, null, null);
        }
    }

    public record ProvidersConfig(OpenAiConfig openai) {
        @JsonCreator
        public ProvidersConfig(@JsonProperty("openai") OpenAiConfig openai) {
            this.openai = openai == null ? OpenAiConfig.defaults() : openai;
        }

        public static ProvidersConfig defaults() {
            return new ProvidersConfig(null);
        }
    }

    public record OpenAiConfig(
            String apiKey,
            String baseUrl,
            String model,
            String reasoningEffort,
            Integer timeoutSeconds
    ) {
        @JsonCreator
        public OpenAiConfig(
                @JsonProperty("apiKey") String apiKey,
                @JsonProperty("baseUrl") String baseUrl,
                @JsonProperty("model") String model,
                @JsonProperty("reasoningEffort") String reasoningEffort,
                @JsonProperty("timeoutSeconds") Integer timeoutSeconds
        ) {
            this.apiKey = apiKey == null ? "" : apiKey;
            this.baseUrl = baseUrl == null ? "https://api.openai.com" : baseUrl;
            this.model = model == null ? "" : model;
            this.reasoningEffort = reasoningEffort == null ? "low" : reasoningEffort;
            this.timeoutSeconds = timeoutSeconds == null ? 90 : timeoutSeconds;
        }

        public static OpenAiConfig defaults() {
            return new OpenAiConfig(null, null, null, null, null);
        }
    }

    public record GitHubConfig(
            String apiBaseUrl,
            String owner,
            String repo,
            String token,
            String defaultBaseBranch
    ) {
        @JsonCreator
        public GitHubConfig(
                @JsonProperty("apiBaseUrl") String apiBaseUrl,
                @JsonProperty("owner") String owner,
                @JsonProperty("repo") String repo,
                @JsonProperty("token") String token,
                @JsonProperty("defaultBaseBranch") String defaultBaseBranch
        ) {
            this.apiBaseUrl = apiBaseUrl == null ? "https://api.github.com" : apiBaseUrl;
            this.owner = owner == null ? "" : owner;
            this.repo = repo == null ? "" : repo;
            this.token = token == null ? "" : token;
            this.defaultBaseBranch = defaultBaseBranch == null ? "main" : defaultBaseBranch;
        }

        public static GitHubConfig defaults() {
            return new GitHubConfig(null, null, null, null, null);
        }
    }

    public record DemoConfig(String scenariosDir) {
        @JsonCreator
        public DemoConfig(@JsonProperty("scenariosDir") String scenariosDir) {
            this.scenariosDir = scenariosDir == null ? "demo/scenarios" : scenariosDir;
        }

        public static DemoConfig defaults() {
            return new DemoConfig(null);
        }
    }
}
