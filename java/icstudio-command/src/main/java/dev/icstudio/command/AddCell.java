package dev.icstudio.command;

import dev.icstudio.project.Project;
import java.util.Objects;

public record AddCell(String library, String name) implements ProjectCommand {
    public AddCell {
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Project apply(Project project) {
        return project.withCellAdded(library, name);
    }
}
