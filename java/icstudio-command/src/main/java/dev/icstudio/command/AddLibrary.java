package dev.icstudio.command;

import dev.icstudio.project.Project;
import java.util.Objects;

public record AddLibrary(String name) implements ProjectCommand {
    public AddLibrary {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Project apply(Project project) {
        return project.withLibraryAdded(name);
    }
}
