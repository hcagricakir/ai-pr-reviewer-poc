package dev.prreviewer.diff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReviewInput(
        String sourceType,
        String sourceId,
        String title,
        List<DiffSection> diffSections,
        Map<String, String> metadata
) {
    public ReviewInput {
        diffSections = diffSections == null ? List.of() : List.copyOf(diffSections);
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    public String combinedDiff() {
        return diffSections.stream()
                .map(DiffSection::diffText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
