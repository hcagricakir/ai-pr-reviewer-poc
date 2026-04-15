package dev.prreviewer.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Severity {
    CRITICAL("critical"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low"),
    INFO("info");

    private final String apiValue;

    Severity(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static Severity fromApiValue(String value) {
        for (Severity severity : values()) {
            if (severity.apiValue.equalsIgnoreCase(value)) {
                return severity;
            }
        }
        return INFO;
    }
}
