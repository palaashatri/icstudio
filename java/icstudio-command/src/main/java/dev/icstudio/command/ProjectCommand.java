package dev.icstudio.command;

import dev.icstudio.project.Project;

/** Typed project mutation applied to an immutable project value. */
public sealed interface ProjectCommand permits AddLibrary, AddCell, AddView {
    Project apply(Project project);
}
