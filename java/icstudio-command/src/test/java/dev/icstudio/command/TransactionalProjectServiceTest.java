package dev.icstudio.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.core.Revision;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class TransactionalProjectServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void oneCommandBatchProducesExactlyOneRevision() throws Exception {
        var service = TransactionalProjectService.create(temporaryDirectory.resolve("demo"), "demo");

        var revision = service.commit(
                Revision.ZERO,
                "request-1",
                "test",
                List.of(
                        new AddLibrary("analog"),
                        new AddCell("analog", "inverter"),
                        new AddView("analog", "inverter", "schematic", "schematic"),
                        new AddView("analog", "inverter", "symbol", "symbol")));

        assertEquals(new Revision(1L), revision);
        assertEquals(new Revision(1L), service.snapshot().revision());
        assertEquals(1, service.snapshot().hierarchyCounts().libraries());
        assertEquals(1, service.snapshot().hierarchyCounts().cells());
        assertEquals(2, service.snapshot().hierarchyCounts().views());
    }

    @Test
    void invalidCommandLeavesAuthoritativeStateUntouched() throws Exception {
        var service = TransactionalProjectService.create(temporaryDirectory.resolve("atomic"), "demo");
        var before = service.snapshot();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.commit(
                        Revision.ZERO,
                        "request-invalid",
                        "test",
                        List.of(new AddCell("missing", "inverter"))));

        assertEquals(before, service.snapshot());
    }

    @Test
    void requestIdentityAndActorAreMandatory() throws Exception {
        var service = TransactionalProjectService.create(temporaryDirectory.resolve("metadata"), "demo");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.commit(
                        Revision.ZERO, "", "test", List.of(new AddLibrary("analog"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.commit(
                        Revision.ZERO, "request", " ", List.of(new AddLibrary("analog"))));
    }
}
