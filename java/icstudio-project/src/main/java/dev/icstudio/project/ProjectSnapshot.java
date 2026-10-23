package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import dev.icstudio.core.Revision;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, revision-addressed project state exposed to UI, CLI, SDK, and MCP. */
public record ProjectSnapshot(
        int schemaVersion,
        ObjectId id,
        String name,
        Revision revision,
        NavigableMap<String, Library> libraries) {

    public ProjectSnapshot {
        if (schemaVersion != Project.FORMAT_VERSION) {
            throw new IllegalArgumentException("unsupported project schema version " + schemaVersion);
        }
        Objects.requireNonNull(id, "id");
        name = ProjectNames.component("project", name);
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(libraries, "libraries");
        libraries = Collections.unmodifiableNavigableMap(new TreeMap<>(libraries));
    }

    public HierarchyCounts hierarchyCounts() {
        int cells = 0;
        int views = 0;
        for (var library : libraries.values()) {
            cells += library.cells().size();
            for (var cell : library.cells().values()) {
                views += cell.views().size();
            }
        }
        return new HierarchyCounts(libraries.size(), cells, views);
    }

    public String summaryJson() {
        var counts = hierarchyCounts();
        return "{"
                + "\"schemaVersion\":" + schemaVersion
                + ",\"projectId\":\"" + id.toHex() + "\""
                + ",\"name\":\"" + escapeJson(name) + "\""
                + ",\"revision\":" + revision.toUnsignedString()
                + ",\"libraries\":" + counts.libraries()
                + ",\"cells\":" + counts.cells()
                + ",\"views\":" + counts.views()
                + "}";
    }

    private static String escapeJson(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        return output.toString();
    }
}
