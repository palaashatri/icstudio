package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.icstudio.cli.IcStudioCli;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Replays each recorded CLI invocation and compares status, stdout, and stderr with the Rust binary. */
class CliOracleTest {
    private static final String USAGE_MARKER_A = "  license-check";
    private static final String USAGE_MARKER_B = "  sbom --output";

    @TempDir
    Path temporary;

    @Test
    void everyRecordedInvocationMatchesTheRustCli() throws IOException {
        Path programme = OracleCorpus.copyTree(OracleCorpus.resolve("evidence/programme"), temporary.resolve("programme"));
        Path base = OracleCorpus.projectCopy(OracleCorpus.resolve("project/base.icst"), temporary.resolve("base"));
        Path finalRoot = OracleCorpus.projectCopy(OracleCorpus.resolve("project/expected-final.icst"), temporary.resolve("final"));

        for (String name : caseNames()) {
            var arguments = new ArrayList<String>();
            for (String line : Files.readAllLines(OracleCorpus.resolve("cli/" + name + "/args"), StandardCharsets.UTF_8)) {
                arguments.add(line.replace(OracleCorpus.PROGRAMME_PLACEHOLDER, programme.toString())
                        .replace(OracleCorpus.BASE_PLACEHOLDER, base.toString())
                        .replace(OracleCorpus.FINAL_PLACEHOLDER, finalRoot.toString()));
            }
            var stdout = new ByteArrayOutputStream();
            var stderr = new ByteArrayOutputStream();
            int status = IcStudioCli.run(arguments.toArray(String[]::new),
                    new PrintStream(stdout, true, StandardCharsets.UTF_8),
                    new PrintStream(stderr, true, StandardCharsets.UTF_8));

            assertEquals(Integer.parseInt(read(name, "exit").strip()), status, name + " exit status");
            assertEquals(comparable(read(name, "stdout")), comparable(stdout.toString(StandardCharsets.UTF_8)), name + " stdout");
            assertEquals(comparable(read(name, "stderr")), comparable(stderr.toString(StandardCharsets.UTF_8)), name + " stderr");
        }
    }

    private static List<String> caseNames() throws IOException {
        var names = new ArrayList<String>();
        try (var directories = Files.list(OracleCorpus.resolve("cli"))) {
            directories.filter(Files::isDirectory).forEach(path -> names.add(path.getFileName().toString()));
        }
        names.sort(null);
        return names;
    }

    private static String read(String name, String file) throws IOException {
        return OracleCorpus.normalizeNewlines(Files.readString(OracleCorpus.resolve("cli/" + name + "/" + file), StandardCharsets.UTF_8));
    }

    /**
     * Removes the trailing command list from usage text. The Java CLI replaces the Rust {@code sbom}
     * command with {@code license-check --sbom}; every other byte of the usage and all messages must match.
     */
    static String comparable(String text) {
        String normalized = OracleCorpus.normalizeNewlines(text);
        int cut = normalized.length();
        for (String marker : List.of(USAGE_MARKER_A, USAGE_MARKER_B)) {
            int index = normalized.indexOf(marker);
            if (index >= 0) {
                cut = Math.min(cut, index);
            }
        }
        return normalized.substring(0, cut);
    }
}
