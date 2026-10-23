package dev.icstudio.command;

import dev.icstudio.project.Project;
import java.util.List;
import java.util.Objects;

/** Applies an ordered command batch atomically to an immutable project value. */
public final class CommandBus {
    public Project apply(Project project, List<? extends ProjectCommand> commands) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(commands, "commands");
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("transaction must contain at least one command");
        }

        Project next = project;
        for (var command : commands) {
            next = Objects.requireNonNull(command, "command").apply(next);
        }
        return next.withRevision(project.revision().next());
    }
}
