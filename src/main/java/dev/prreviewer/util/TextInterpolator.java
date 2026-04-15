package dev.prreviewer.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextInterpolator {

    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Z0-9_]+)(?::([^}]*))?}");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([A-Z0-9_]+)}}");

    private TextInterpolator() {
    }

    public static String interpolateEnvironment(String input, Map<String, String> values) {
        Matcher matcher = ENV_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2) == null ? "" : matcher.group(2);
            String replacement = values.getOrDefault(key, defaultValue);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String interpolateTemplate(String template, Map<String, String> values) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = values.getOrDefault(key, "");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
