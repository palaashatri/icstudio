package dev.icstudio.project;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * File-backed project store for deterministic snapshots.
 *
 * <p>Write-ahead journaling and crash recovery are intentionally implemented by
 * the command layer in the next migration slice. This class establishes the
 * authoritative path and durable snapshot primitive used by that layer.</p>
 */
public final class ProjectStore {
    public static final String STATE_DIRECTORY = ".icstudio";
    public static final String SNAPSHOT_FILE = "project.icst";
    public static final String SNAPSHOT_TEMP_FILE = "project.icst.tmp";

    private final Path root;
    private Project project;

    private ProjectStore(Path root, Project project) {
        this.root = root;
        this.project = project;
    }

    public static ProjectStore create(Path root, String name) throws IOException {
        Objects.requireNonNull(root, "root");
        Path normalized = root.toAbsolutePath().normalize();
        Files.createDirectories(statePath(normalized));
        if (Files.exists(snapshotPath(normalized))) {
            throw new IOException("project already exists at " + normalized);
        }

        var project = Project.create(name);
        writeSnapshot(normalized, ProjectCodec.encode(project));
        return new ProjectStore(normalized, project);
    }

    public static ProjectStore open(Path root) throws IOException {
        Objects.requireNonNull(root, "root");
        Path normalized = root.toAbsolutePath().normalize();
        Path snapshot = snapshotPath(normalized);
        if (!Files.isRegularFile(snapshot)) {
            throw new IOException("no project exists at " + normalized);
        }

        String text = Files.readString(snapshot, StandardCharsets.UTF_8);
        return new ProjectStore(normalized, ProjectCodec.decode(text));
    }

    public Path root() {
        return root;
    }

    public Project project() {
        return project;
    }

    public ProjectSnapshot snapshot() {
        return project.snapshot();
    }

    public void save(Project next) throws IOException {
        Objects.requireNonNull(next, "next");
        if (!project.id().equals(next.id())) {
            throw new IllegalArgumentException("cannot replace a project with a different project id");
        }

        writeSnapshot(root, ProjectCodec.encode(next));
        project = next;
    }

    public static Path snapshotPath(Path root) {
        return statePath(root).resolve(SNAPSHOT_FILE);
    }

    private static Path statePath(Path root) {
        return root.resolve(STATE_DIRECTORY);
    }

    private static void writeSnapshot(Path root, String contents) throws IOException {
        Path state = statePath(root);
        Files.createDirectories(state);
        Path temporary = state.resolve(SNAPSHOT_TEMP_FILE);
        Path snapshot = snapshotPath(root);

        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        try (var channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }

        try {
            Files.move(
                    temporary,
                    snapshot,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, snapshot, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
