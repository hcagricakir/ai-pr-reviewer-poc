package dev.prreviewer.ai;

import dev.prreviewer.diff.DiffLineLocator;
import dev.prreviewer.diff.DiffSection;
import dev.prreviewer.review.NormalizedReviewPayload;
import dev.prreviewer.review.ReviewAction;
import dev.prreviewer.review.ReviewContext;
import dev.prreviewer.review.ReviewFinding;
import dev.prreviewer.review.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public final class MockReviewProvider implements AiReviewProvider {

    private static final Pattern VAGUE_NAMES = Pattern.compile("\\b(tmp|data|val|x1|obj|res|doStuff)\\b");

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public NormalizedReviewPayload review(ReviewContext context) {
        List<ReviewFinding> findings = new ArrayList<>();
        for (DiffSection section : context.reviewInput().diffSections()) {
            String diffText = section.diffText();
            String lowered = diffText.toLowerCase();
            String filePath = section.changedFile().path();

            if (lowered.contains("orelse(null)") || lowered.contains("return null;") || lowered.contains(".get()")) {
                findings.add(createFinding(
                        "high",
                        "Nullable path can leak into runtime failure",
                        "The change reintroduces nullable control flow or unchecked Optional access.",
                        "This can turn a valid edge case into a NullPointerException or undefined behavior under missing data.",
                        "Keep Optional semantics intact or guard the nullable branch before dereferencing.",
                        0.88,
                        filePath,
                        diffText,
                        lowered.contains("orelse(null)") ? "orElse(null)" : lowered.contains("return null;") ? "return null;" : ".get()"
                ));
            }

            if (VAGUE_NAMES.matcher(diffText).find()) {
                findings.add(createFinding(
                        "medium",
                        "Naming hides intent",
                        "The introduced identifiers are too vague to explain the purpose of the logic.",
                        "Ambiguous names slow down review and make later refactors riskier because intent is not encoded in the code.",
                        "Rename the identifiers to reflect the business meaning of the values or operation.",
                        0.74,
                        filePath,
                        diffText,
                        "tmp"
                ));
            }

            if ((lowered.contains("sendemail(") || lowered.contains("publish(") || lowered.contains("charge("))
                    && (lowered.contains("repository") || lowered.contains("save(") || lowered.contains("client."))) {
                findings.add(createFinding(
                        "medium",
                        "Too many responsibilities in one change unit",
                        "The same code path now mixes orchestration with side effects like persistence, external calls, or notifications.",
                        "This makes failures harder to isolate and increases the chance of inconsistent state when one side effect succeeds and another fails.",
                        "Separate orchestration from persistence or notification concerns so each piece has one clear reason to change.",
                        0.79,
                        filePath,
                        diffText,
                        lowered.contains("sendemail(") ? "sendEmail(" : lowered.contains("publish(") ? "publish(" : "charge("
                ));
            }

            if ((lowered.contains("for (") || lowered.contains("foreach"))
                    && (lowered.contains("repository.") || lowered.contains("client.") || lowered.contains("findbyid(") || lowered.contains("load("))) {
                findings.add(createFinding(
                        "medium",
                        "Potential repeated expensive call inside loop",
                        "The diff appears to call a repository or client method from inside an iteration.",
                        "That pattern often scales poorly and can turn a simple request into N+1 I/O or repeated remote calls under load.",
                        "Prefetch the required data, batch the lookup, or move the expensive call outside the loop when possible.",
                        0.83,
                        filePath,
                        diffText,
                        lowered.contains("findbyid(") ? "findById(" : lowered.contains("repository.") ? "repository." : "client."
                ));
            }
        }

        if (findings.isEmpty()) {
            return new NormalizedReviewPayload(
                    "No actionable findings based on the current review heuristics.",
                    "no_findings",
                    ReviewAction.COMMENT,
                    List.of(),
                    List.of("Mock provider was used. Replace with OpenAI for model-backed review.")
            );
        }

        boolean shouldRequestChanges = findings.stream()
                .map(ReviewFinding::severity)
                .anyMatch(severity -> severity == Severity.CRITICAL || severity == Severity.HIGH);

        return new NormalizedReviewPayload(
                shouldRequestChanges
                        ? "The change set contains blocking issues that should be fixed before merge."
                        : "The change set contains actionable issues worth addressing before merge.",
                shouldRequestChanges ? "blocked" : "concerns",
                shouldRequestChanges ? ReviewAction.REQUEST_CHANGES : ReviewAction.COMMENT,
                findings,
                List.of("Mock provider was used. Findings are heuristic and intended for local flow validation.")
        );
    }

    private ReviewFinding createFinding(
            String severity,
            String title,
            String problem,
            String whyItMatters,
            String recommendation,
            double confidence,
            String filePath,
            String diffText,
            String needle
    ) {
        OptionalInt line = DiffLineLocator.locateStartLine(diffText, needle);
        Integer startLine = line.isPresent() ? line.getAsInt() : null;
        return new ReviewFinding(
                severity,
                title,
                problem,
                whyItMatters,
                recommendation,
                confidence,
                filePath,
                startLine,
                startLine
        );
    }
}
