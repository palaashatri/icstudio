package dev.icstudio.ui.workspace;

import dev.icstudio.project.Cell;
import dev.icstudio.project.Library;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.project.View;
import java.awt.BorderLayout;
import java.util.Enumeration;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** Keyboard-navigable tree of the project's libraries, cells, and views. */
public final class LibraryNavigator extends JPanel {
    private final JTree tree;

    public LibraryNavigator() {
        super(new BorderLayout());
        tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("No project")));
        tree.getAccessibleContext().setAccessibleName("Library navigator");
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    /** Replaces the tree with the snapshot's hierarchy. The snapshot is read, never modified. */
    public void bind(ProjectSnapshot snapshot) {
        var root = new DefaultMutableTreeNode(snapshot.name());
        for (Library library : snapshot.libraries().values()) {
            var libraryNode = new DefaultMutableTreeNode(library.name());
            for (Cell cell : library.cells().values()) {
                var cellNode = new DefaultMutableTreeNode(cell.name());
                for (View view : cell.views().values()) {
                    cellNode.add(new DefaultMutableTreeNode(view.name() + " (" + view.kind() + ")"));
                }
                libraryNode.add(cellNode);
            }
            root.add(libraryNode);
        }
        tree.setModel(new DefaultTreeModel(root));
        tree.expandRow(0);
        tree.setSelectionPath(new TreePath(root.getPath()));
    }

    /** Number of nodes in the bound tree, including the project root. */
    public int nodeCount() {
        var root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();
        int count = 0;
        Enumeration<?> nodes = root.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            nodes.nextElement();
            count++;
        }
        return count;
    }
}
