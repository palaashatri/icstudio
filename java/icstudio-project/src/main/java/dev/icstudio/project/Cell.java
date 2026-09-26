package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable cell and its deterministically ordered views. */
public record Cell(ObjectId id, String name, NavigableMap<String, View> views) {
    public Cell {
        Objects.requireNonNull(id, "id");
        name = ProjectNames.component("cell", name);
        views = immutableSorted(views);
        for (var entry : views.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("view map key must equal view name");
            }
        }
    }

    public Cell withView(String viewName, String kind) {
        ProjectNames.component("view", viewName);
        ProjectNames.component("view kind", kind);
        if (views.containsKey(viewName)) {
            throw new IllegalArgumentException("view '" + name + "/" + viewName + "' already exists");
        }

        var next = new TreeMap<>(views);
        var id = ProjectIds.derive(this.id, "view/" + viewName + "/" + kind);
        next.put(viewName, new View(id, viewName, kind));
        return new Cell(this.id, name, next);
    }

    private static NavigableMap<String, View> immutableSorted(NavigableMap<String, View> source) {
        Objects.requireNonNull(source, "views");
        return Collections.unmodifiableNavigableMap(new TreeMap<>(source));
    }
}
