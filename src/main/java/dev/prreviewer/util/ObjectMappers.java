package dev.prreviewer.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ObjectMappers {

    private ObjectMappers() {
    }

    public static ObjectMapper yaml() {
        return base(new ObjectMapper(new YAMLFactory()));
    }

    public static ObjectMapper json() {
        return base(new ObjectMapper());
    }

    private static ObjectMapper base(ObjectMapper objectMapper) {
        return objectMapper
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
