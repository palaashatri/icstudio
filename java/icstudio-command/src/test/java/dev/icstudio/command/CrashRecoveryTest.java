package dev.icstudio.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.core.Revision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CrashRecoveryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readyJournalIsReplayedAfterInjectedTermination() throws Exception {
        var root = temporaryDirectory.resolve("recovery");
        var service = TransactionalProjectService.create(root, "demo");

        var error = assertThrows(
                IOException.class,
                () -> service.commit(
                        Revision.ZERO,
                        "request-recovery",
                        "test",
                        List.of(new AddLibrary("recovered")),
                        CommitFailpoint.AFTER_JOURNAL_READY));
        assertTrue(error.getMessage().contains("injected termination"));
        assertEquals(Revision.ZERO, service.snapshot().revision());

        var reopened = TransactionalProjectService.open(root);
        assertEquals(new Revision(1L), reopened.snapshot().revision());
        assertTrue(reopened.snapshot().libraries().containsKey("recovered"));

        try (var journalFiles = Files.list(
                root.resolve(".icstudio").resolve(Journal.JOURNAL_DIRECTORY))) {
            assertEquals(0L, journalFiles.count());
        }
    }

    @Test
    void incompleteTemporaryJournalIsDiscarded() throws Exception {
        var root = temporaryDirectory.resolve("tmp");
        TransactionalProjectService.create(root, "demo");
        var journal = root.resolve(".icstudio").resolve(Journal.JOURNAL_DIRECTORY);
        Files.createDirectories(journal);
        Files.writeString(journal.resolve("revision-00000000000000000001.tmp"), "partial");

        var reopened = TransactionalProjectService.open(root);

        assertEquals(Revision.ZERO, reopened.snapshot().revision());
        assertTrue(Files.notExists(journal.resolve("revision-00000000000000000001.tmp")));
    }
}
