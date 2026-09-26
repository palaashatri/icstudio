package dev.icstudio.command;

import dev.icstudio.core.Revision;
import dev.icstudio.project.Project;
import dev.icstudio.project.ProjectCodec;
import dev.icstudio.project.ProjectStore;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;

/** Write-ahead snapshot journal compatible with the M1 persistence protocol. */
public final class Journal {
    public static final String JOURNAL_DIRECTORY = "journal";

    private final Path root;

    public Journal(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(journalPath(this.root));
    }

    public Path publish(Project next) throws IOException {
        String stem = "revision-" + paddedRevision(next.revision());
        Path directory = journalPath(root);
        Path temporary = directory.resolve(stem + ".tmp");
        Path ready = directory.resolve(stem + ".ready");

        writeSynced(temporary, ProjectCodec.encode(next));
        try {
            Files.move(
                    temporary,
                    ready,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, ready, StandardCopyOption.REPLACE_EXISTING);
        }
        return ready;
    }

    public void retire(Revision revision) throws IOException {
        Files.deleteIfExists(readyPath(revision));
    }

    public static void recover(Path root) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        Path directory = journalPath(normalized);
        Files.createDirectories(directory);

        try (var stream = Files.list(directory)) {
            for (var path : stream.filter(path -> path.getFileName().toString().endsWith(".tmp")).toList()) {
                Files.deleteIfExists(path);
            }
        }

        var ready = new ArrayList<Project>();
        try (var stream = Files.list(directory)) {
            for (var path : stream.filter(path -> path.getFileName().toString().endsWith(".ready")).toList()) {
                ready.add(ProjectCodec.decode(Files.readString(path, StandardCharsets.UTF_8)));
            }
        }
        ready.sort(Comparator.comparing(Project::revision));

        if (!ready.isEmpty()) {
            var store = ProjectStore.open(normalized);
            for (var candidate : ready) {
                if (candidate.revision().compareTo(store.project().revision()) > 0) {
                    store.save(candidate);
                }
            }
        }

        try (var stream = Files.list(directory)) {
            for (var path : stream.filter(path -> path.getFileName().toString().endsWith(".ready")).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private Path readyPath(Revision revision) {
        return journalPath(root).resolve("revision-" + paddedRevision(revision) + ".ready");
    }

    private static Path journalPath(Path root) {
        return root.resolve(ProjectStore.STATE_DIRECTORY).resolve(JOURNAL_DIRECTORY);
    }

    private static String paddedRevision(Revision revision) {
        return "0".repeat(20 - revision.toUnsignedString().length()) + revision.toUnsignedString();
    }

    private static void writeSynced(Path path, String contents) throws IOException {
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        try (var channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }
}
