package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import java.util.Objects;

/** Immutable project view identity and kind. */
public record View(ObjectId id, String name, String kind) {
    public View {
        Objects.requireNonNull(id, "id");
        name = ProjectNames.component("view", name);
        kind = ProjectNames.component("view kind", kind);
    }
}
