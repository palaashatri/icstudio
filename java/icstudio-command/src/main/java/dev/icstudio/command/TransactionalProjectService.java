package dev.icstudio.command;

import dev.icstudio.core.Revision;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.project.ProjectStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Revision-safe transactional façade shared by future UI, CLI, SDK, and MCP adapters. */
public final class TransactionalProjectService {
    private final ProjectStore store;
    private final Journal journal;
    private final CommandBus commandBus;

    private TransactionalProjectService(ProjectStore store) throws IOException {
        this.store = store;
        this.journal = new Journal(store.root());
        this.commandBus = new CommandBus();
    }

    public static TransactionalProjectService create(Path root, String name) throws IOException {
        return new TransactionalProjectService(ProjectStore.create(root, name));
    }

    public static TransactionalProjectService open(Path root) throws IOException {
        Journal.recover(root);
        return new TransactionalProjectService(ProjectStore.open(root));
    }

    public ProjectSnapshot snapshot() {
        return store.snapshot();
    }

    public Revision commit(
            Revision expectedRevision,
            String requestId,
            String actor,
            List<? extends ProjectCommand> commands) throws IOException {
        return commit(expectedRevision, requestId, actor, commands, CommitFailpoint.NONE);
    }

    public Revision commit(
            Revision expectedRevision,
            String requestId,
            String actor,
            List<? extends ProjectCommand> commands,
            CommitFailpoint failpoint) throws IOException {
        Objects.requireNonNull(expectedRevision, "expectedRevision");
        Objects.requireNonNull(commands, "commands");
        Objects.requireNonNull(failpoint, "failpoint");
        requireNonBlank("request_id", requestId);
        requireNonBlank("actor", actor);

        var current = store.project();
        if (!expectedRevision.equals(current.revision())) {
            throw new IllegalStateException(
                    "revision conflict: expected "
                            + expectedRevision.toUnsignedString()
                            + ", current "
                            + current.revision().toUnsignedString());
        }

        var next = commandBus.apply(current, commands);
        journal.publish(next);

        if (failpoint == CommitFailpoint.AFTER_JOURNAL_READY) {
            throw new IOException("injected termination after journal publication");
        }

        store.save(next);
        journal.retire(next.revision());
        return next.revision();
    }

    private static void requireNonBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transaction " + field + " must not be empty");
        }
    }
}
