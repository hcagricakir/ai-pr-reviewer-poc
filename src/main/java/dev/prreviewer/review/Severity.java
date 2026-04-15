package dev.prreviewer.review;

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

    public String apiValue() {
        return apiValue;
    }

    public static Severity fromApiValue(String value) {
        for (Severity severity : values()) {
            if (severity.apiValue.equalsIgnoreCase(value)) {
                return severity;
            }
        }
        return INFO;
    }
}
