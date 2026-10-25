package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.platform.evidence.ProgrammeEvidence;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Compares truth score, capability report, and state hashes with the recorded programme snapshot. */
class EvidenceOracleTest {
    @TempDir
    Path temporary;

    @Test
    void truthScoreMatchesTheRustSnapshot() throws IOException {
        Path programme = OracleCorpus.resolve("evidence/programme");
        assertEquals(OracleCorpus.read("evidence/truth-score.txt").strip(),
                String.format(Locale.ROOT, "%.2f", ProgrammeEvidence.truthScore(programme)));
    }

    @Test
    void capabilityReportMatchesTheRustSnapshotExactly() throws IOException {
        Path programme = OracleCorpus.resolve("evidence/programme");
        assertEquals(OracleCorpus.read("evidence/capability-report.md"),
                ProgrammeEvidence.capabilityReportMarkdown(programme));
    }

    @Test
    void stateHashesMatchTheRustDigest() throws IOException {
        Path programme = OracleCorpus.resolve("evidence/programme");
        String capabilities = "capabilities " + ProgrammeEvidence.fnv1a64(
                Files.readAllBytes(programme.resolve(ProgrammeEvidence.CAPABILITIES_PATH)));
        String truth = "truth " + ProgrammeEvidence.fnv1a64(
                Files.readAllBytes(programme.resolve(ProgrammeEvidence.TRUTH_PATH)));
        assertEquals(OracleCorpus.read("evidence/fnv1a64.txt"), capabilities + "\n" + truth + "\n");
    }

    @Test
    void checkpointResumesOnlyWhileTheStateIsUnchanged() throws IOException {
        Path root = minimalProgrammeRoot(temporary.resolve("state"));
        ProgrammeEvidence.createCheckpoint(root, "CP-ORACLE");
        ProgrammeEvidence.resumeCheck(root, "CP-ORACLE");
        Path truth = root.resolve(ProgrammeEvidence.TRUTH_PATH);
        Files.writeString(truth, Files.readString(truth, StandardCharsets.UTF_8) + "\n", StandardCharsets.UTF_8);
        var error = assertThrows(IllegalStateException.class, () -> ProgrammeEvidence.resumeCheck(root, "CP-ORACLE"));
        assertTrue(error.getMessage().startsWith("truth state differs from checkpoint CP-ORACLE"));
    }

    @Test
    void checkpointNamesAreImmutableAndScoped() throws IOException {
        Path root = minimalProgrammeRoot(temporary.resolve("names"));
        ProgrammeEvidence.createCheckpoint(root, "CP-ONCE");
        var duplicate = assertThrows(IllegalStateException.class,
                () -> ProgrammeEvidence.createCheckpoint(root, "CP-ONCE"));
        assertTrue(duplicate.getMessage().startsWith("checkpoint already exists"));
        var unscoped = assertThrows(IllegalArgumentException.class,
                () -> ProgrammeEvidence.createCheckpoint(root, "../escape"));
        assertEquals("checkpoint names must start with CP-", unscoped.getMessage());
    }

    /** A temporary root with every file that project-state validation requires. */
    static Path minimalProgrammeRoot(Path root) throws IOException {
        Path programme = OracleCorpus.copyTree(OracleCorpus.resolve("evidence/programme"), root);
        Files.writeString(programme.resolve("AGENTS.md"), "agents\n");
        Files.writeString(programme.resolve("LICENSE"), "MIT\n");
        Files.writeString(programme.resolve("mvnw"), "#!/bin/sh\n");
        Files.writeString(programme.resolve("mvnw.cmd"), "@echo off\n");
        Files.createDirectories(programme.resolve(".mvn/wrapper"));
        Files.writeString(programme.resolve(".mvn/wrapper/maven-wrapper.properties"), "distributionType=bin\n");
        Files.writeString(programme.resolve("pom.xml"),
                "<project><licenses><license><name>MIT License</name></license></licenses></project>\n");
        return programme;
    }
}
