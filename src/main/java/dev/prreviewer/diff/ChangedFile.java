package dev.prreviewer.diff;

public record ChangedFile(
        String path,
        ChangeType changeType,
        int additions,
        int deletions,
        boolean patchAvailable
) {
}
