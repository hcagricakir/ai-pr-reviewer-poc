package dev.prreviewer.cli;

import picocli.CommandLine.Command;

@Command(
        name = "pr-reviewer",
        description = "Policy-driven AI pull request reviewer proof of concept.",
        mixinStandardHelpOptions = true,
        subcommands = {ReviewCommand.class}
)
public final class PrReviewerCli implements Runnable {

    @Override
    public void run() {
        // Picocli shows command usage when no subcommand is provided.
    }
}
