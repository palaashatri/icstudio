package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import dev.icstudio.core.Revision;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Immutable authoritative project hierarchy.
 *
 * <p>Hierarchy editing returns a new value. Revision advancement is deliberately
 * separate so the command/transaction layer can own accepted-transaction semantics.</p>
 */
public final class Project {
    public static final int FORMAT_VERSION = 1;

    private final ObjectId id;
    private final String name;
    private final Revision revision;
    private final NavigableMap<String, Library> libraries;

    private Project(
            ObjectId id,
            String name,
            Revision revision,
            NavigableMap<String, Library> libraries) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = ProjectNames.component("project", name);
        this.revision = Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(libraries, "libraries");

        var sorted = new TreeMap<String, Library>();
        for (var entry : libraries.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("library map key must equal library name");
            }
            sorted.put(entry.getKey(), entry.getValue());
        }
        this.libraries = Collections.unmodifiableNavigableMap(sorted);
    }

    public static Project create(String name) {
        var uuid = UUID.randomUUID();
        return restore(
                new ObjectId(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()),
                name,
                Revision.ZERO,
                new TreeMap<>());
    }

    public static Project restore(
            ObjectId id,
            String name,
            Revision revision,
            NavigableMap<String, Library> libraries) {
        return new Project(id, name, revision, libraries);
    }

    public int schemaVersion() {
        return FORMAT_VERSION;
    }

    public ObjectId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Revision revision() {
        return revision;
    }

    public NavigableMap<String, Library> libraries() {
        return libraries;
    }

    public Project withRevision(Revision nextRevision) {
        Objects.requireNonNull(nextRevision, "nextRevision");
        return new Project(id, name, nextRevision, libraries);
    }

    public Project withLibraryAdded(String libraryName) {
        ProjectNames.component("library", libraryName);
        if (libraries.containsKey(libraryName)) {
            throw new IllegalArgumentException("library '" + libraryName + "' already exists");
        }

        var next = new TreeMap<>(libraries);
        var libraryId = ProjectIds.derive(id, "library/" + libraryName);
        next.put(libraryName, new Library(libraryId, libraryName, new TreeMap<>()));
        return new Project(id, name, revision, next);
    }

    public Project withCellAdded(String libraryName, String cellName) {
        ProjectNames.component("library", libraryName);
        var library = libraries.get(libraryName);
        if (library == null) {
            throw new IllegalArgumentException("library '" + libraryName + "' does not exist");
        }

        var next = new TreeMap<>(libraries);
        next.put(libraryName, library.withCell(cellName));
        return new Project(id, name, revision, next);
    }

    public Project withViewAdded(
            String libraryName,
            String cellName,
            String viewName,
            String kind) {
        ProjectNames.component("library", libraryName);
        var library = libraries.get(libraryName);
        if (library == null) {
            throw new IllegalArgumentException("library '" + libraryName + "' does not exist");
        }

        var next = new TreeMap<>(libraries);
        next.put(libraryName, library.withView(cellName, viewName, kind));
        return new Project(id, name, revision, next);
    }

    public ProjectSnapshot snapshot() {
        return new ProjectSnapshot(FORMAT_VERSION, id, name, revision, libraries);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Project project)) {
            return false;
        }
        return id.equals(project.id)
                && name.equals(project.name)
                && revision.equals(project.revision)
                && libraries.equals(project.libraries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, revision, libraries);
    }

    @Override
    public String toString() {
        return "Project[id=" + id.toHex()
                + ", name=" + name
                + ", revision=" + revision.toUnsignedString()
                + ", libraries=" + libraries.size()
                + "]";
    }
}
