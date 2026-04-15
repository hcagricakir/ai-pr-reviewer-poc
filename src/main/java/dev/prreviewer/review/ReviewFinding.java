package dev.prreviewer.review;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewFinding(
        Severity severity,
        String title,
        String problem,
        String whyItMatters,
        String recommendation,
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
            @JsonProperty("whyItMatters") String whyItMatters,
            @JsonProperty("recommendation") String recommendation,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("startLine") Integer startLine,
            @JsonProperty("endLine") Integer endLine
    ) {
        this(
                Severity.fromApiValue(severity),
                title == null ? "" : title,
                problem == null ? "" : problem,
                whyItMatters == null ? "" : whyItMatters,
                recommendation == null ? "" : recommendation,
                confidence,
                filePath == null ? "" : filePath,
                startLine,
                endLine
        );
    }
}
