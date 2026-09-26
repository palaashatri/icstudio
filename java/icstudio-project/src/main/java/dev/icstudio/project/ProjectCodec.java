package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import dev.icstudio.core.Revision;
import java.util.TreeMap;

/** Deterministic codec for ICStudio native project metadata format version 1. */
public final class ProjectCodec {
    public static final String FORMAT_MAGIC = "ICSTUDIO_PROJECT";

    private ProjectCodec() {
    }

    public static String encode(Project project) {
        var output = new StringBuilder();
        output.append(FORMAT_MAGIC).append('\t').append(Project.FORMAT_VERSION).append('\n');
        output.append("project\t")
                .append(project.id().toHex()).append('\t')
                .append(project.revision().toUnsignedString()).append('\t')
                .append(escapeField(project.name())).append('\n');

        for (var library : project.libraries().values()) {
            output.append("library\t")
                    .append(library.id().toHex()).append('\t')
                    .append(escapeField(library.name())).append('\n');

            for (var cell : library.cells().values()) {
                output.append("cell\t")
                        .append(cell.id().toHex()).append('\t')
                        .append(escapeField(library.name())).append('\t')
                        .append(escapeField(cell.name())).append('\n');

                for (var view : cell.views().values()) {
                    output.append("view\t")
                            .append(view.id().toHex()).append('\t')
                            .append(escapeField(library.name())).append('\t')
                            .append(escapeField(cell.name())).append('\t')
                            .append(escapeField(view.name())).append('\t')
                            .append(escapeField(view.kind())).append('\n');
                }
            }
        }
        return output.toString();
    }

    public static Project decode(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("project file is empty");
        }

        String[] lines = input.split("\n", -1);
        if (lines.length < 2 || !(FORMAT_MAGIC + "\t1").equals(lines[0])) {
            throw new IllegalArgumentException(
                    "unsupported project header '" + (lines.length == 0 ? "" : lines[0]) + "'");
        }

        String[] projectFields = lines[1].split("\t", -1);
        if (projectFields.length != 4 || !"project".equals(projectFields[0])) {
            throw new IllegalArgumentException("invalid project record");
        }

        var projectId = ObjectId.parseHex(projectFields[1]);
        var revision = Revision.parseUnsigned(projectFields[2]);
        var projectName = ProjectNames.component("project", unescapeField(projectFields[3]));
        var libraries = new TreeMap<String, Library>();

        for (int lineIndex = 2; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split("\t", -1);
            switch (fields[0]) {
                case "library" -> {
                    requireFieldCount(fields, 3, lineIndex);
                    var name = ProjectNames.component("library", unescapeField(fields[2]));
                    if (libraries.containsKey(name)) {
                        throw new IllegalArgumentException("duplicate library '" + name + "'");
                    }
                    libraries.put(name, new Library(
                            ObjectId.parseHex(fields[1]),
                            name,
                            new TreeMap<>()));
                }
                case "cell" -> {
                    requireFieldCount(fields, 4, lineIndex);
                    var libraryName = ProjectNames.component("library", unescapeField(fields[2]));
                    var cellName = ProjectNames.component("cell", unescapeField(fields[3]));
                    var library = libraries.get(libraryName);
                    if (library == null) {
                        throw new IllegalArgumentException(
                                "cell references missing library '" + libraryName + "'");
                    }
                    if (library.cells().containsKey(cellName)) {
                        throw new IllegalArgumentException(
                                "duplicate cell '" + libraryName + "/" + cellName + "'");
                    }
                    var cells = new TreeMap<>(library.cells());
                    cells.put(cellName, new Cell(
                            ObjectId.parseHex(fields[1]),
                            cellName,
                            new TreeMap<>()));
                    libraries.put(libraryName, new Library(library.id(), library.name(), cells));
                }
                case "view" -> {
                    requireFieldCount(fields, 6, lineIndex);
                    var libraryName = ProjectNames.component("library", unescapeField(fields[2]));
                    var cellName = ProjectNames.component("cell", unescapeField(fields[3]));
                    var viewName = ProjectNames.component("view", unescapeField(fields[4]));
                    var kind = ProjectNames.component("view kind", unescapeField(fields[5]));

                    var library = libraries.get(libraryName);
                    var cell = library == null ? null : library.cells().get(cellName);
                    if (cell == null) {
                        throw new IllegalArgumentException(
                                "view references missing cell '" + libraryName + "/" + cellName + "'");
                    }
                    if (cell.views().containsKey(viewName)) {
                        throw new IllegalArgumentException(
                                "duplicate view '" + libraryName + "/" + cellName + "/" + viewName + "'");
                    }

                    var views = new TreeMap<>(cell.views());
                    views.put(viewName, new View(ObjectId.parseHex(fields[1]), viewName, kind));
                    var cells = new TreeMap<>(library.cells());
                    cells.put(cellName, new Cell(cell.id(), cell.name(), views));
                    libraries.put(libraryName, new Library(library.id(), library.name(), cells));
                }
                default -> throw new IllegalArgumentException(
                        "invalid project record at line " + (lineIndex + 1) + ": '" + line + "'");
            }
        }

        return Project.restore(projectId, projectName, revision, libraries);
    }

    private static void requireFieldCount(String[] fields, int expected, int zeroBasedLineIndex) {
        if (fields.length != expected) {
            throw new IllegalArgumentException(
                    "invalid project record at line " + (zeroBasedLineIndex + 1));
        }
    }

    static String escapeField(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> output.append("\\\\");
                case '\t' -> output.append("\\t");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                default -> output.append(character);
            }
        }
        return output.toString();
    }

    static String unescapeField(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                output.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IllegalArgumentException("unterminated project field escape");
            }
            char escaped = value.charAt(index);
            switch (escaped) {
                case '\\' -> output.append('\\');
                case 't' -> output.append('\t');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                default -> throw new IllegalArgumentException(
                        "unsupported project field escape '\\" + escaped + "'");
            }
        }
        return output.toString();
    }
}
