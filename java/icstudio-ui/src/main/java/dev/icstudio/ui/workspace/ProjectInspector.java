package dev.icstudio.ui.workspace;

import dev.icstudio.project.HierarchyCounts;
import dev.icstudio.project.ProjectSnapshot;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Read-only property table for the active project: identity, revision, and hierarchy counts. */
public final class ProjectInspector extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new Object[] {"Property", "Value"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public ProjectInspector() {
        super(new BorderLayout());
        var table = new JTable(model);
        table.getAccessibleContext().setAccessibleName("Project inspector");
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void bind(ProjectSnapshot snapshot) {
        HierarchyCounts counts = snapshot.hierarchyCounts();
        model.setRowCount(0);
        for (String[] row : rows(snapshot, counts)) {
            model.addRow(row);
        }
    }

    static List<String[]> rows(ProjectSnapshot snapshot, HierarchyCounts counts) {
        return List.of(
                new String[] {"Name", snapshot.name()},
                new String[] {"Project ID", snapshot.id().toHex()},
                new String[] {"Revision", snapshot.revision().toUnsignedString()},
                new String[] {"Libraries", Integer.toString(counts.libraries())},
                new String[] {"Cells", Integer.toString(counts.cells())},
                new String[] {"Views", Integer.toString(counts.views())});
    }

    public int rowCount() {
        return model.getRowCount();
    }

    /** The property/value pairs currently displayed, in display order. */
    public java.util.Map<String, String> displayedProperties() {
        var properties = new java.util.LinkedHashMap<String, String>();
        for (int row = 0; row < model.getRowCount(); row++) {
            properties.put((String) model.getValueAt(row, 0), (String) model.getValueAt(row, 1));
        }
        return java.util.Collections.unmodifiableMap(properties);
    }
}
