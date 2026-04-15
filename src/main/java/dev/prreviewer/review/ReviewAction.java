package dev.prreviewer.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReviewAction {
    COMMENT("comment", "COMMENT"),
    REQUEST_CHANGES("request_changes", "REQUEST_CHANGES");

    private final String apiValue;
    private final String githubEvent;

    ReviewAction(String apiValue, String githubEvent) {
        this.apiValue = apiValue;
        this.githubEvent = githubEvent;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    public String githubEvent() {
        return githubEvent;
    }

    @JsonCreator
    public static ReviewAction fromApiValue(String value) {
        for (ReviewAction action : values()) {
            if (action.apiValue.equalsIgnoreCase(value)) {
                return action;
            }
        }
        return COMMENT;
    }
}
