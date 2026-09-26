package dev.icstudio.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.core.Revision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ProjectStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createSaveReopenPreservesHierarchyIdsAndRevision() throws Exception {
        var store = ProjectStore.create(temporaryDirectory.resolve("demo"), "demo");
        var next = store.project()
                .withLibraryAdded("analog")
                .withCellAdded("analog", "inverter")
                .withViewAdded("analog", "inverter", "schematic", "schematic")
                .withViewAdded("analog", "inverter", "symbol", "symbol")
                .withRevision(new Revision(1L));

        store.save(next);

        var reopened = ProjectStore.open(temporaryDirectory.resolve("demo"));
        assertEquals(next, reopened.project());
        assertEquals(new HierarchyCounts(1, 1, 2), reopened.snapshot().hierarchyCounts());
        assertTrue(Files.isRegularFile(ProjectStore.snapshotPath(reopened.root())));
        assertFalse(Files.exists(
                reopened.root().resolve(ProjectStore.STATE_DIRECTORY).resolve(ProjectStore.SNAPSHOT_TEMP_FILE)));
    }

    @Test
    void refusesToOverwriteAnExistingProjectDuringCreate() throws Exception {
        var root = temporaryDirectory.resolve("existing");
        ProjectStore.create(root, "demo");

        assertThrows(IOException.class, () -> ProjectStore.create(root, "other"));
    }

    @Test
    void refusesToSaveDifferentProjectIdentity() throws Exception {
        var store = ProjectStore.create(temporaryDirectory.resolve("identity"), "demo");
        var other = Project.create("other");

        assertThrows(IllegalArgumentException.class, () -> store.save(other));
    }

    @Test
    void openRejectsMissingProject() {
        assertThrows(
                IOException.class,
                () -> ProjectStore.open(temporaryDirectory.resolve("missing")));
    }
}
