package dev.icstudio.results;

import dev.icstudio.core.TextLines;
import dev.icstudio.core.Utf8Order;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Immutable result manifest plus named signals, validated against the M1 limits. */
public final class ResultSet {
    static final int MAX_SAMPLES_PER_SIGNAL = 10_000_000;

    private final RunManifest manifest;
    private final NavigableMap<String, Signal> signals = new TreeMap<>(Utf8Order.COMPARATOR);

    public ResultSet(RunManifest manifest, java.util.Collection<Signal> values) {
        this.manifest = manifest;
        for (var signal : values) {
            if (signals.put(signal.name(), signal) != null) {
                throw new IllegalArgumentException("duplicate signal '" + signal.name() + "'");
            }
        }
        validate();
    }

    public RunManifest manifest() {
        return manifest;
    }

    public NavigableMap<String, Signal> signals() {
        return Collections.unmodifiableNavigableMap(signals);
    }

    public void validate() {
        validateToken("run_id", manifest.runId());
        validateToken("project_id", manifest.projectId());
        validateToken("analysis", manifest.analysis());
        validateToken("input_hash", manifest.inputHash());
        validateToken("solver", manifest.solver());
        validateToken("solver_version", manifest.solverVersion());
        for (var entry : signals.entrySet()) {
            var signal = entry.getValue();
            if (!entry.getKey().equals(signal.name())) {
                throw new IllegalArgumentException("signal map key '" + entry.getKey() + "' is inconsistent");
            }
            validateToken("signal", signal.name());
            validateToken("unit", signal.unit());
            if (signal.axis().size() != signal.values().size()) {
                throw new IllegalArgumentException("signal '" + signal.name() + "' axis/value length mismatch: "
                        + signal.axis().size() + " versus " + signal.values().size());
            }
            for (int index = 1; index < signal.axis().size(); index++) {
                if (signal.axis().get(index - 1) > signal.axis().get(index)) {
                    throw new IllegalArgumentException("signal '" + signal.name() + "' axis is not monotonic");
                }
            }
            if (signal.axis().size() > MAX_SAMPLES_PER_SIGNAL) {
                throw new IllegalArgumentException("signal '" + signal.name() + "' exceeds the M1 sample limit");
            }
        }
    }

    public String summaryJson() {
        validate();
        long samples = signals.values().stream().mapToLong(signal -> signal.values().size()).sum();
        return "{\"runId\":\"" + escapeJson(manifest.runId())
                + "\",\"projectId\":\"" + escapeJson(manifest.projectId())
                + "\",\"projectRevision\":" + manifest.projectRevision()
                + ",\"analysis\":\"" + escapeJson(manifest.analysis())
                + "\",\"solver\":\"" + escapeJson(manifest.solver())
                + "\",\"signalCount\":" + signals.size()
                + ",\"sampleCount\":" + samples + "}";
    }

    private static void validateToken(String kind, String value) {
        if (value == null || value.isEmpty() || Utf8Order.utf8Length(value) > 256 || TextLines.hasControl(value)) {
            throw new IllegalArgumentException(kind + " must be 1 to 256 printable characters");
        }
    }

    static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
