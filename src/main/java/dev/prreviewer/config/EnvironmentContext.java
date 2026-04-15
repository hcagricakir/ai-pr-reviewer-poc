package dev.prreviewer.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvironmentContext {

    private final Map<String, String> values;

    private EnvironmentContext(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static EnvironmentContext load(Path projectRoot) {
        Map<String, String> resolvedValues = new LinkedHashMap<>();
        Dotenv dotenv = Dotenv.configure()
                .directory(projectRoot.toAbsolutePath().toString())
                .filename(".env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> resolvedValues.put(entry.getKey(), entry.getValue()));
        resolvedValues.putAll(System.getenv());
        return new EnvironmentContext(resolvedValues);
    }

    public Map<String, String> values() {
        return values;
    }
}
