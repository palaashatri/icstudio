package dev.icstudio.ui.workspace;

import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.ui.scene.SkiaScenePanel;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * The M1 project workbench: navigator, Skia engineering canvas, and inspector.
 *
 * <p>The workbench binds an immutable {@link ProjectSnapshot}. It holds presentation state only
 * (selection, scroll, layout) and never owns or mutates the authoritative project.</p>
 */
public final class ProjectWorkbench extends JPanel {
    private final LibraryNavigator navigator = new LibraryNavigator();
    private final SkiaScenePanel canvas = new SkiaScenePanel();
    private final ProjectInspector inspector = new ProjectInspector();
    private ProjectSnapshot snapshot;

    public ProjectWorkbench() {
        super(new BorderLayout());
        var centre = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigator, canvas);
        centre.setResizeWeight(0.2);
        var right = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centre, inspector);
        right.setResizeWeight(0.85);
        right.setBorder(BorderFactory.createEmptyBorder());
        add(right, BorderLayout.CENTER);
    }

    public void bind(ProjectSnapshot next) {
        this.snapshot = Objects.requireNonNull(next, "snapshot");
        navigator.bind(next);
        canvas.bind(next);
        inspector.bind(next);
    }

    public ProjectSnapshot snapshot() {
        return snapshot;
    }

    public LibraryNavigator navigator() {
        return navigator;
    }

    public SkiaScenePanel canvas() {
        return canvas;
    }

    public ProjectInspector inspector() {
        return inspector;
    }
}
