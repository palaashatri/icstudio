package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.command.AddCell;
import dev.icstudio.command.AddLibrary;
import dev.icstudio.command.AddView;
import dev.icstudio.command.CommandBus;
import dev.icstudio.command.ProjectCommand;
import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.core.Revision;
import dev.icstudio.project.Project;
import dev.icstudio.project.ProjectCodec;
import dev.icstudio.project.ProjectStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Replays the Rust oracle's project transactions and compares bytes and rejection messages. */
class ProjectOracleTest {
    @TempDir
    Path temporary;

    @Test
    void decodedBaseProjectMatchesTheRustSummary() throws IOException {
        Path root = OracleCorpus.projectCopy(OracleCorpus.resolve("project/base.icst"), temporary.resolve("base"));
        var store = ProjectStore.open(root);

        assertEquals(OracleCorpus.read("project/base-summary.json"), store.snapshot().summaryJson());
        assertEquals(
                OracleCorpus.read("project/base.icst"),
                ProjectCodec.encode(store.project()),
                "re-encoding the base project must reproduce the Rust bytes exactly");
    }

    @Test
    void replayedTransactionsReproduceTheRustFinalBytesAndSummary() throws IOException {
        Path root = OracleCorpus.projectCopy(OracleCorpus.resolve("project/base.icst"), temporary.resolve("replay"));
        var service = TransactionalProjectService.open(root);

        for (String line : OracleCorpus.read("project/sequence.txt").split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            var fields = line.split("\\|", -1);
            var revision = Revision.parseUnsigned(fields[0]);
            service.commit(revision, "oracle-" + fields[0], "oracle", List.of(command(fields)));
        }

        Path finalSnapshot = root.resolve(".icstudio/project.icst");
        assertEquals(
                OracleCorpus.read("project/expected-final.icst"),
                Files.readString(finalSnapshot, StandardCharsets.UTF_8));
        assertEquals(OracleCorpus.read("project/expected-summary.json"), service.snapshot().summaryJson());
    }

    @Test
    void rejectionsMatchTheRustMessagesExactly() throws IOException {
        Path root = OracleCorpus.projectCopy(OracleCorpus.resolve("project/base.icst"), temporary.resolve("rejections"));
        var service = TransactionalProjectService.open(root);
        for (String line : OracleCorpus.read("project/sequence.txt").split("\n")) {
            if (!line.isBlank()) {
                var fields = line.split("\\|", -1);
                service.commit(Revision.parseUnsigned(fields[0]), "oracle", "oracle", List.of(command(fields)));
            }
        }
        Project finalProject = ProjectStore.open(root).project();

        for (String line : OracleCorpus.read("project/errors.tsv").split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            int tab = line.indexOf('\t');
            String spec = line.substring(0, tab);
            String expected = line.substring(tab + 1);
            var fields = spec.split("\\|", -1);
            var error = assertThrows(RuntimeException.class, () -> rejectionFor(finalProject, fields));
            assertEquals(expected, error.getMessage(), spec);
        }
    }

    @Test
    void mutationsRequireTheCurrentRevision() throws IOException {
        Path root = OracleCorpus.projectCopy(OracleCorpus.resolve("project/base.icst"), temporary.resolve("stale"));
        var service = TransactionalProjectService.open(root);
        var error = assertThrows(IllegalStateException.class, () ->
                service.commit(new Revision(5), "stale", "oracle", List.of(new AddLibrary("digital"))));
        assertEquals("revision conflict: expected 5, current 0", error.getMessage());
        assertEquals(Revision.ZERO, service.snapshot().revision());
    }

    private static void rejectionFor(Project project, String[] fields) {
        var revision = Revision.parseUnsigned(fields[0]);
        if (!revision.equals(project.revision())) {
            throw new IllegalStateException("revision conflict: expected " + revision.toUnsignedString()
                    + ", current " + project.revision().toUnsignedString());
        }
        new CommandBus().apply(project, List.of(command(fields)));
    }

    static ProjectCommand command(String[] fields) {
        return switch (fields[1]) {
            case "add-library" -> new AddLibrary(fields[2]);
            case "add-cell" -> new AddCell(fields[2], fields[3]);
            case "add-view" -> new AddView(fields[2], fields[3], fields[4], fields[5]);
            default -> throw new IllegalArgumentException("unsupported oracle transaction " + fields[1]);
        };
    }
}
