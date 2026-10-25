package dev.icstudio.ui.workspace;

import dev.icstudio.project.Cell;
import dev.icstudio.project.Library;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.project.View;
import java.util.ArrayList;
import java.util.List;

/** Pure, testable projection of a project snapshot into navigator rows. */
public final class ProjectNavigatorModel {
    /** One navigator row: its depth in the hierarchy and its label. */
    public record Row(int depth, String label, String kind) {
    }

    private ProjectNavigatorModel() {
    }

    public static List<Row> rows(ProjectSnapshot snapshot) {
        var rows = new ArrayList<Row>();
        rows.add(new Row(0, snapshot.name(), "project"));
        for (Library library : snapshot.libraries().values()) {
            rows.add(new Row(1, library.name(), "library"));
            for (Cell cell : library.cells().values()) {
                rows.add(new Row(2, cell.name(), "cell"));
                for (View view : cell.views().values()) {
                    rows.add(new Row(3, view.name() + " (" + view.kind() + ")", "view"));
                }
            }
        }
        return List.copyOf(rows);
    }
}
