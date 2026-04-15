package dev.prreviewer.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceLoader {

    private ResourceLoader() {
    }

    public static String readClasspathResource(String resourcePath) {
        try (InputStream inputStream = ResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read classpath resource: " + resourcePath, exception);
        }
    }
}
