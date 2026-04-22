package dev.prreviewer.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewFinding(
        Severity severity,
        String title,
        String problem,
        String codeSuggestion,
        double confidence,
        String filePath,
        Integer startLine,
        Integer endLine
) {

    @JsonCreator
    public ReviewFinding(
            @JsonProperty("severity") String severity,
            @JsonProperty("title") String title,
            @JsonProperty("problem") String problem,
            @JsonProperty("codeSuggestion") String codeSuggestion,
            @JsonProperty("confidence") Double confidence,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("startLine") Integer startLine,
            @JsonProperty("endLine") Integer endLine
    ) {
        this(
                Severity.fromApiValue(severity),
                title == null ? "" : title,
                problem == null ? "" : problem,
                codeSuggestion == null ? "" : codeSuggestion.stripTrailing(),
                confidence == null ? 1.0d : confidence,
                filePath == null ? "" : filePath,
                startLine,
                endLine
        );
    }
}
