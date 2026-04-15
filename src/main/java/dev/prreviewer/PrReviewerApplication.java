package dev.prreviewer;

import dev.prreviewer.cli.PrReviewerCli;
import picocli.CommandLine;

public final class PrReviewerApplication {

    private PrReviewerApplication() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PrReviewerCli()).execute(args);
        System.exit(exitCode);
    }
}
