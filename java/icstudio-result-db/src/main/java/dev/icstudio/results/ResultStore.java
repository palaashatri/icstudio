package dev.icstudio.results;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic file store for one run's result manifest. */
public final class ResultStore {
    public static final String MANIFEST = "manifest.icres";

    private ResultStore() {
    }

    public static Path save(Path root, ResultSet results) throws IOException {
        String serialized = ResultCodec.serialize(results);
        Files.createDirectories(root);
        Path destination = root.resolve(MANIFEST);
        Path temporary = root.resolve(MANIFEST + ".tmp");
        Files.writeString(temporary, serialized, StandardCharsets.UTF_8);
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    public static ResultSet open(Path root) throws IOException {
        return ResultCodec.deserialize(Files.readString(root.resolve(MANIFEST), StandardCharsets.UTF_8));
    }
}
