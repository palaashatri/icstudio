package dev.icstudio.experiment;

import java.util.Objects;

/**
 * Immutable description of one planned analysis run, bound to a project revision.
 *
 * <p>This is the experiment-domain skeleton. No solver is attached and no run is executed.</p>
 */
public record RunPlan(String runId, String projectId, long projectRevision, String analysis) {
    public RunPlan {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(analysis, "analysis");
        requireToken("runId", runId);
        requireToken("projectId", projectId);
        requireToken("analysis", analysis);
    }

    private static void requireToken(String field, String value) {
        if (value.isEmpty() || value.length() > 256 || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " must be 1 to 256 printable characters");
        }
    }
}
