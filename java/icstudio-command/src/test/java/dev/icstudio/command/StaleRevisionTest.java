package dev.icstudio.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.core.Revision;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class StaleRevisionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void staleRevisionIsRejectedWithoutMutation() throws Exception {
        var service = TransactionalProjectService.create(temporaryDirectory.resolve("stale"), "demo");
        service.commit(
                Revision.ZERO,
                "request-1",
                "test",
                List.of(new AddLibrary("analog")));
        var before = service.snapshot();

        var error = assertThrows(
                IllegalStateException.class,
                () -> service.commit(
                        Revision.ZERO,
                        "request-stale",
                        "test",
                        List.of(new AddLibrary("stale"))));

        assertEquals(
                "revision conflict: expected 0, current 1",
                error.getMessage());
        assertEquals(before, service.snapshot());
    }
}
