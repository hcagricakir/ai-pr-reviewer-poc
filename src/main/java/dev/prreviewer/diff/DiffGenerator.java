package dev.prreviewer.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import java.util.Arrays;
import java.util.List;

public final class DiffGenerator {

    public DiffSection generate(String path, ChangeType changeType, String baseContent, String headContent) {
        List<String> original = splitLines(baseContent);
        List<String> revised = splitLines(headContent);
        Patch<String> patch = DiffUtils.diff(original, revised);

        String oldPath = switch (changeType) {
            case ADDED -> "/dev/null";
            case MODIFIED, REMOVED, RENAMED -> "a/" + path;
        };
        String newPath = switch (changeType) {
            case REMOVED -> "/dev/null";
            case ADDED, MODIFIED, RENAMED -> "b/" + path;
        };

        List<String> unifiedLines = UnifiedDiffUtils.generateUnifiedDiff(oldPath, newPath, original, patch, 3);
        String diffText = String.join("\n", unifiedLines);
        int additions = patch.getDeltas().stream().mapToInt(delta -> delta.getTarget().size()).sum();
        int deletions = patch.getDeltas().stream().mapToInt(delta -> delta.getSource().size()).sum();
        boolean patchAvailable = !patch.getDeltas().isEmpty();

        return new DiffSection(
                new ChangedFile(path, changeType, additions, deletions, patchAvailable),
                diffText
        );
    }

    private List<String> splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(content.replace("\r\n", "\n").split("\n", -1));
    }
}
