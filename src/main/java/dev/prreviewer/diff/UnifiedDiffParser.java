package dev.prreviewer.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UnifiedDiffParser {

    public List<DiffSection> parse(String diffText) {
        List<String> lines = Arrays.asList(diffText.replace("\r\n", "\n").split("\n", -1));
        List<DiffSection> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("diff --git ") && !current.isEmpty()) {
                sections.add(toSection(current.toString().stripTrailing()));
                current = new StringBuilder();
            }
            current.append(line).append('\n');
        }

        if (!current.isEmpty()) {
            sections.add(toSection(current.toString().stripTrailing()));
        }
        return sections;
    }

    private DiffSection toSection(String sectionText) {
        String path = "";
        ChangeType changeType = ChangeType.MODIFIED;
        int additions = 0;
        int deletions = 0;
        boolean patchAvailable = sectionText.contains("@@");

        for (String line : sectionText.split("\n")) {
            if (line.startsWith("diff --git ")) {
                String[] tokens = line.split(" ");
                if (tokens.length >= 4) {
                    path = tokens[3].replaceFirst("^b/", "");
                }
            } else if (line.startsWith("--- /dev/null")) {
                changeType = ChangeType.ADDED;
            } else if (line.startsWith("+++ /dev/null")) {
                changeType = ChangeType.REMOVED;
            } else if (line.startsWith("rename from ")) {
                changeType = ChangeType.RENAMED;
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                additions++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                deletions++;
            } else if (line.startsWith("+++ b/") && path.isBlank()) {
                path = line.substring("+++ b/".length());
            }
        }

        return new DiffSection(
                new ChangedFile(path, changeType, additions, deletions, patchAvailable),
                sectionText
        );
    }
}
