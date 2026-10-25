package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.worker.RpcEnvelope;
import dev.icstudio.worker.WorkerProcess;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A worker crash must neither terminate the platform nor corrupt the authoritative project. */
class CrashIsolationTest {
    @TempDir
    Path temporary;

    @Test
    void workerCrashLeavesTheProjectIntact() throws Exception {
        Path project = OracleCorpus.projectCopy(OracleCorpus.resolve("project/expected-final.icst"), temporary.resolve("p"));
        String before = TransactionalProjectService.open(project).snapshot().summaryJson();

        var outcome = WorkerProcess.exchange(new RpcEnvelope.Request(
                "crash-1", "0123456789abcdef0123456789abcdef", 0, "worker.crash", ""));
        assertTrue(outcome.crashed());
        assertTrue(outcome.exitCode() != 0);

        assertEquals(before, TransactionalProjectService.open(project).snapshot().summaryJson());
        assertEquals(OracleCorpus.read("project/expected-summary.json"),
                TransactionalProjectService.open(project).snapshot().summaryJson());
    }
}
