package dev.prreviewer.config;

import dev.prreviewer.util.ObjectMappers;
import dev.prreviewer.util.TextInterpolator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    private final EnvironmentContext environmentContext;

    public ConfigLoader(EnvironmentContext environmentContext) {
        this.environmentContext = environmentContext;
    }

    public ApplicationConfig loadApplicationConfig(Path path) {
        return load(path, ApplicationConfig.class);
    }

    public AgentProfile loadAgentProfile(Path path) {
        return load(path, AgentProfile.class);
    }

    public <T> T load(Path path, Class<T> type) {
        try {
            String rawContent = Files.readString(path);
            String interpolated = TextInterpolator.interpolateEnvironment(rawContent, environmentContext.values());
            return ObjectMappers.yaml().readValue(interpolated, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load YAML file: " + path, exception);
        }
    }
}
