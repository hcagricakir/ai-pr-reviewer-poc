package dev.prreviewer.diff;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ReviewInputFilter {

    public ReviewInput apply(
            ReviewInput input,
            List<String> includePatterns,
            List<String> excludePatterns,
            int maxFiles,
            int maxDiffCharacters
    ) {
        List<DiffSection> acceptedSections = new ArrayList<>();
        int totalCharacters = 0;
        int skippedBySize = 0;
        int skippedByPattern = 0;

        for (DiffSection section : input.diffSections()) {
            if (!matches(section.changedFile().path(), includePatterns, excludePatterns)) {
                skippedByPattern++;
                continue;
            }
            if (acceptedSections.size() >= maxFiles) {
                skippedBySize++;
                continue;
            }
            if (totalCharacters + section.diffText().length() > maxDiffCharacters) {
                skippedBySize++;
                continue;
            }
            acceptedSections.add(section);
            totalCharacters += section.diffText().length();
        }

        Map<String, String> metadata = new LinkedHashMap<>(input.metadata());
        if (skippedByPattern > 0) {
            metadata.put("skippedByPattern", Integer.toString(skippedByPattern));
        }
        if (skippedBySize > 0) {
            metadata.put("skippedByLimits", Integer.toString(skippedBySize));
        }

        return new ReviewInput(input.sourceType(), input.sourceId(), input.title(), acceptedSections, metadata);
    }

    private boolean matches(String filePath, List<String> includePatterns, List<String> excludePatterns) {
        Path path = Path.of(filePath);
        boolean included = includePatterns == null || includePatterns.isEmpty() || includePatterns.stream()
                .anyMatch(pattern -> matchesGlob(path, pattern));
        boolean excluded = excludePatterns != null && excludePatterns.stream()
                .anyMatch(pattern -> matchesGlob(path, pattern));
        return included && !excluded;
    }

    private boolean matchesGlob(Path path, String pattern) {
        return candidatePatterns(pattern)
                .map(candidate -> FileSystems.getDefault().getPathMatcher("glob:" + candidate))
                .anyMatch(pathMatcher -> pathMatcher.matches(path));
    }

    private Stream<String> candidatePatterns(String pattern) {
        if (!pattern.startsWith("**/")) {
            return Stream.of(pattern);
        }
        return Stream.of(pattern, pattern.substring(3));
    }
}
