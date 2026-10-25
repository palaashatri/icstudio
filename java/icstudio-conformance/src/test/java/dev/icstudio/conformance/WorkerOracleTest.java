package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.worker.RpcEnvelope;
import dev.icstudio.worker.WorkerProcess;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Runs real worker JVMs and compares their envelopes, statuses, and crash containment with Rust. */
class WorkerOracleTest {
    @Test
    void pingEnvelopeMatchesTheRustWorker() throws Exception {
        var request = new RpcEnvelope.Request(
                "ping-1", "0123456789abcdef0123456789abcdef", 7, "worker.ping", "health-check");
        var outcome = WorkerProcess.exchange(request);

        assertEquals(0, outcome.exitCode());
        assertEquals(OracleCorpus.read("worker/ping.stdout").strip(), outcome.response().encode());
    }

    @Test
    void unknownCommandIsReportedAsAnErrorResponse() throws Exception {
        var request = new RpcEnvelope.Request(
                "unknown-1", "0123456789abcdef0123456789abcdef", 1, "worker.nope", "");
        var outcome = WorkerProcess.exchange(request);

        assertEquals(0, outcome.exitCode());
        assertFalse(outcome.response().success());
        assertEquals(OracleCorpus.read("worker/unknown-command.stdout").strip(), outcome.response().encode());
    }

    @Test
    void crashKillsOnlyTheWorkerAndReportsNoResponse() throws Exception {
        var request = new RpcEnvelope.Request(
                "crash-1", "0123456789abcdef0123456789abcdef", 0, "worker.crash", "");
        var outcome = WorkerProcess.exchange(request);

        assertNotEquals(0, outcome.exitCode());
        assertTrue(outcome.crashed());
    }

    @Test
    void malformedEnvelopeExitsWithTheRustStatus() throws Exception {
        var process = new ProcessBuilder(WorkerProcess.javaCommand())
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write((OracleCorpus.read("worker/bad-version.input")).getBytes(StandardCharsets.UTF_8));
        }
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(64, process.waitFor());
        // The JVM may announce JAVA_TOOL_OPTIONS on stderr; only the worker's own diagnostic is compared.
        String diagnostic = stderr.lines()
                .filter(line -> !line.startsWith("Picked up JAVA_TOOL_OPTIONS"))
                .collect(Collectors.joining("\n"));
        assertEquals(OracleCorpus.read("worker/bad-version.stderr").strip(), diagnostic.strip());
    }
}
