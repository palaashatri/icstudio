package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable library and its deterministically ordered cells. */
public record Library(ObjectId id, String name, NavigableMap<String, Cell> cells) {
    public Library {
        Objects.requireNonNull(id, "id");
        name = ProjectNames.component("library", name);
        cells = immutableSorted(cells);
        for (var entry : cells.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("cell map key must equal cell name");
            }
        }
    }

    public Library withCell(String cellName) {
        ProjectNames.component("cell", cellName);
        if (cells.containsKey(cellName)) {
            throw new IllegalArgumentException("cell '" + name + "/" + cellName + "' already exists");
        }

        var next = new TreeMap<>(cells);
        var id = ProjectIds.derive(this.id, "cell/" + cellName);
        next.put(cellName, new Cell(id, cellName, new TreeMap<>()));
        return new Library(this.id, name, next);
    }

    public Library withView(String cellName, String viewName, String kind) {
        var cell = cells.get(cellName);
        if (cell == null) {
            throw new IllegalArgumentException("cell '" + name + "/" + cellName + "' does not exist");
        }
        var next = new TreeMap<>(cells);
        next.put(cellName, cell.withView(viewName, kind));
        return new Library(id, name, next);
    }

    private static NavigableMap<String, Cell> immutableSorted(NavigableMap<String, Cell> source) {
        Objects.requireNonNull(source, "cells");
        return Collections.unmodifiableNavigableMap(new TreeMap<>(source));
    }
}
