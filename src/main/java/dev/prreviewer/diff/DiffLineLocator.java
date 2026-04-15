package dev.prreviewer.diff;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiffLineLocator {

    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*$");

    private DiffLineLocator() {
    }

    public static OptionalInt locateStartLine(String diffText, String needle) {
        int currentNewLine = -1;
        for (String line : diffText.split("\n")) {
            Matcher matcher = HUNK_HEADER.matcher(line);
            if (matcher.matches()) {
                currentNewLine = Integer.parseInt(matcher.group(1));
                continue;
            }
            if (currentNewLine < 0) {
                continue;
            }
            if (line.startsWith("+") || line.startsWith(" ")) {
                if (line.substring(1).contains(needle)) {
                    return OptionalInt.of(currentNewLine);
                }
                currentNewLine++;
                continue;
            }
            if (line.startsWith("-")) {
                if (line.substring(1).contains(needle)) {
                    return OptionalInt.of(currentNewLine);
                }
            }
        }
        return OptionalInt.empty();
    }
}
