package dev.icstudio.conformance;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Access to the recorded Rust M1 oracle corpus and to temporary copies of its project trees.
 *
 * <p>The corpus is read-only evidence. Anything that can mutate a project, such as the journal
 * recovery performed on open, runs on a copy so the recorded bytes never change.</p>
 */
final class OracleCorpus {
    static final String PROGRAMME_PLACEHOLDER = "@PROGRAMME@";
    static final String FINAL_PLACEHOLDER = "@FINAL@";
    static final String BASE_PLACEHOLDER = "@BASE@";
    static final String RUST_CLAIM = "Java 25 Swing migration in progress; Rust M1 retained as conformance oracle";

    private OracleCorpus() {
    }

    static Path root() {
        try {
            return Path.of(OracleCorpus.class.getResource("/rust-oracle").toURI());
        } catch (URISyntaxException error) {
            throw new IllegalStateException("oracle corpus is not on the test classpath", error);
        }
    }

    static Path resolve(String relative) {
        return root().resolve(relative);
    }

    static String read(String relative) throws IOException {
        return Files.readString(resolve(relative));
    }

    /** Lists case names (file stems) in a directory that have the given extension, sorted. */
    static List<String> cases(String relativeDirectory, String extension) throws IOException {
        var names = new ArrayList<String>();
        try (Stream<Path> files = Files.list(resolve(relativeDirectory))) {
            files.filter(path -> path.getFileName().toString().endsWith("." + extension))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        names.add(name.substring(0, name.length() - extension.length() - 1));
                    });
        }
        names.sort(null);
        return names;
    }

    static Path copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return target;
    }

    /** Copies a project snapshot file into {@code target/.icstudio/project.icst} and returns the root. */
    static Path projectCopy(Path snapshotFile, Path target) throws IOException {
        Path state = target.resolve(".icstudio");
        Files.createDirectories(state);
        Files.copy(snapshotFile, state.resolve("project.icst"), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    static String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n");
    }
}
