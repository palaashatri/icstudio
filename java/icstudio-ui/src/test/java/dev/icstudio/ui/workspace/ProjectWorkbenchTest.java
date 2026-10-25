package dev.icstudio.ui.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.icstudio.command.AddCell;
import dev.icstudio.command.AddLibrary;
import dev.icstudio.command.AddView;
import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.core.Revision;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.project.ProjectStore;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectWorkbenchTest {
    @TempDir
    Path temporary;

    @Test
    void workbenchBindsAnImmutableSnapshotAndProjectsTheHierarchy() throws Exception {
        ProjectSnapshot snapshot = sampleSnapshot();
        var workbench = new ProjectWorkbench();
        workbench.bind(snapshot);

        assertSame(snapshot, workbench.snapshot());
        // root + library + cell + view
        assertEquals(4, workbench.navigator().nodeCount());
        assertEquals(6, workbench.inspector().rowCount());
        assertEquals(1, workbench.canvas().scene().boxes().size());
    }

    @Test
    void navigatorRowsFollowTheHierarchyOrder() throws Exception {
        var rows = ProjectNavigatorModel.rows(sampleSnapshot());

        assertEquals(List.of("scene", "analog", "inverter", "schematic (schematic)"),
                rows.stream().map(ProjectNavigatorModel.Row::label).toList());
        assertEquals(List.of(0, 1, 2, 3), rows.stream().map(ProjectNavigatorModel.Row::depth).toList());
    }

    private ProjectSnapshot sampleSnapshot() throws Exception {
        ProjectStore.create(temporary, "scene");
        var service = TransactionalProjectService.open(temporary);
        service.commit(Revision.ZERO, "wb-1", "test", List.of(new AddLibrary("analog")));
        service.commit(new Revision(1), "wb-2", "test", List.of(new AddCell("analog", "inverter")));
        service.commit(new Revision(2), "wb-3", "test", List.of(new AddView("analog", "inverter", "schematic", "schematic")));
        return service.snapshot();
    }
}
