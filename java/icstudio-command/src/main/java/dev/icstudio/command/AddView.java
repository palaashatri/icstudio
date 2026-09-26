package dev.icstudio.command;

import dev.icstudio.project.Project;
import java.util.Objects;

public record AddView(String library, String cell, String name, String kind) implements ProjectCommand {
    public AddView {
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
    }

    @Override
    public Project apply(Project project) {
        return project.withViewAdded(library, cell, name, kind);
    }
}
