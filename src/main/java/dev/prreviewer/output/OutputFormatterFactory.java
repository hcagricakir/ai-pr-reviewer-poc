package dev.prreviewer.output;

public final class OutputFormatterFactory {

    public ReviewOutputFormatter create(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> new JsonReviewOutputFormatter();
            case "markdown" -> new MarkdownReviewOutputFormatter();
            default -> throw new IllegalArgumentException("Unsupported output format: " + format);
        };
    }
}
