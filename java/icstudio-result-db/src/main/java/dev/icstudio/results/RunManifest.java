package dev.icstudio.results;

/** Provenance for one analysis run: the project revision and solver identity that produced it. */
public record RunManifest(
        String runId,
        String projectId,
        long projectRevision,
        String analysis,
        String inputHash,
        String solver,
        String solverVersion) {
}
