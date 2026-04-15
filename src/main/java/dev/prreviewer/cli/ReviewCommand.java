package dev.prreviewer.cli;

import dev.prreviewer.ai.AiProviderFactory;
import dev.prreviewer.config.AgentProfile;
import dev.prreviewer.config.ApplicationConfig;
import dev.prreviewer.config.ConfigLoader;
import dev.prreviewer.config.EnvironmentContext;
import dev.prreviewer.diff.ReviewInput;
import dev.prreviewer.diff.ReviewInputFilter;
import dev.prreviewer.diff.ReviewInputLoader;
import dev.prreviewer.output.OutputFormatterFactory;
import dev.prreviewer.policy.PolicyLoader;
import dev.prreviewer.policy.ResolvedPolicySet;
import dev.prreviewer.review.PromptTemplateRenderer;
import dev.prreviewer.review.ReviewContext;
import dev.prreviewer.review.ReviewOrchestrator;
import dev.prreviewer.review.ReviewReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "review",
        description = "Run AI review on a diff file, a GitHub pull request, or a prepared demo scenario.",
        mixinStandardHelpOptions = true
)
public final class ReviewCommand implements Callable<Integer> {

    @Option(names = "--config", description = "Path to runtime config YAML.")
    private Path configPath;

    @Option(names = "--agent-profile", description = "Path to agent profile YAML.")
    private String agentProfilePath;

    @Option(names = "--diff-file", description = "Path to a unified diff file.")
    private Path diffFile;

    @Option(names = "--changes-manifest", description = "Path to a scenario/change manifest YAML.")
    private Path changesManifest;

    @Option(names = "--github-pr", description = "GitHub pull request number.")
    private Integer githubPullRequest;

    @Option(names = "--sample", description = "Sample scenario id under demo/scenarios.")
    private String sampleScenario;

    @Option(names = "--provider", description = "Provider override. Supported: mock, openai.")
    private String providerOverride;

    @Option(names = "--output-format", description = "Output format override. Supported: markdown, json.")
    private String outputFormatOverride;

    @Option(names = "--dry-run", description = "Print the assembled prompts without calling the AI provider.")
    private boolean dryRun;

    @Override
    public Integer call() {
        validateSingleSourceSelection();

        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        EnvironmentContext environmentContext = EnvironmentContext.load(projectRoot);
        ConfigLoader configLoader = new ConfigLoader(environmentContext);
        ApplicationConfig applicationConfig = configLoader.loadApplicationConfig(resolveConfigPath(projectRoot));
        AgentProfile agentProfile = configLoader.loadAgentProfile(resolveAgentProfilePath(projectRoot, applicationConfig));

        ResolvedPolicySet policySet = new PolicyLoader().load(resolvePolicyPaths(projectRoot, agentProfile));
        ReviewInputLoader reviewInputLoader = new ReviewInputLoader(projectRoot);
        ReviewInput rawInput = loadReviewInput(reviewInputLoader, applicationConfig, projectRoot);
        ReviewInput reviewInput = new ReviewInputFilter().apply(
                rawInput,
                applicationConfig.review().includePatterns(),
                applicationConfig.review().excludePatterns(),
                applicationConfig.review().maxFiles(),
                applicationConfig.review().maxDiffCharacters()
        );

        if (reviewInput.diffSections().isEmpty()) {
            throw new IllegalArgumentException("No eligible diff sections remained after filtering and limits.");
        }

        ReviewOrchestrator reviewOrchestrator = new ReviewOrchestrator(new PromptTemplateRenderer(projectRoot));
        ReviewContext reviewContext = reviewOrchestrator.prepareContext(reviewInput, agentProfile, policySet);

        if (dryRun) {
            System.out.println("# System Prompt\n");
            System.out.println(reviewContext.systemPrompt());
            System.out.println("\n# User Prompt\n");
            System.out.println(reviewContext.userPrompt());
            return 0;
        }

        String providerName = providerOverride == null ? applicationConfig.review().provider() : providerOverride;
        ReviewReport report = reviewOrchestrator.review(
                new AiProviderFactory().create(providerName, applicationConfig),
                reviewInput,
                agentProfile,
                policySet
        );

        String outputFormat = outputFormatOverride != null
                ? outputFormatOverride
                : applicationConfig.review().outputFormat();
        System.out.println(new OutputFormatterFactory().create(outputFormat).format(report));
        return 0;
    }

    private ReviewInput loadReviewInput(
            ReviewInputLoader reviewInputLoader,
            ApplicationConfig applicationConfig,
            Path projectRoot
    ) {
        if (diffFile != null) {
            return reviewInputLoader.loadFromDiffFile(projectRoot.resolve(diffFile).normalize());
        }
        if (changesManifest != null) {
            return reviewInputLoader.loadFromScenarioManifest(projectRoot.resolve(changesManifest).normalize());
        }
        if (githubPullRequest != null) {
            return reviewInputLoader.loadFromGitHubPullRequest(githubPullRequest, applicationConfig.github());
        }
        return reviewInputLoader.loadSample(sampleScenario, applicationConfig.demo().scenariosDir());
    }

    private List<Path> resolvePolicyPaths(Path projectRoot, AgentProfile agentProfile) {
        return agentProfile.review().activePolicies().stream()
                .map(path -> projectRoot.resolve(path).normalize())
                .toList();
    }

    private Path resolveConfigPath(Path projectRoot) {
        if (configPath != null) {
            return projectRoot.resolve(configPath).normalize();
        }
        Path primary = projectRoot.resolve("config/application.yml");
        if (Files.exists(primary)) {
            return primary;
        }
        return projectRoot.resolve("config/application.example.yml");
    }

    private Path resolveAgentProfilePath(Path projectRoot, ApplicationConfig applicationConfig) {
        String profileLocation = agentProfilePath == null
                ? applicationConfig.review().agentProfile()
                : agentProfilePath;
        return projectRoot.resolve(profileLocation).normalize();
    }

    private void validateSingleSourceSelection() {
        int selectedSources = 0;
        selectedSources += diffFile == null ? 0 : 1;
        selectedSources += changesManifest == null ? 0 : 1;
        selectedSources += githubPullRequest == null ? 0 : 1;
        selectedSources += sampleScenario == null ? 0 : 1;

        if (selectedSources != 1) {
            throw new IllegalArgumentException(
                    "Choose exactly one review input source: --diff-file, --changes-manifest, --github-pr, or --sample."
            );
        }
    }
}
