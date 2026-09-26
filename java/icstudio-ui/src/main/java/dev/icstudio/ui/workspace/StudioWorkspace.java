package dev.icstudio.ui.workspace;

import dev.icstudio.ui.material.MaterialSupport;
import dev.icstudio.ui.material.StudioMaterial;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Top-level reusable workspace container.
 *
 * <p>It owns only presentation composition. Engineering state belongs to domain
 * services outside the Swing tree.</p>
 */
public final class StudioWorkspace extends JPanel {
    private final MaterialSupport materials;
    private JComponent toolbar;
    private JComponent sidebar;
    private JComponent content;
    private JComponent inspector;
    private JComponent statusBar;

    public StudioWorkspace() {
        this(MaterialSupport.solidFallback());
    }

    public StudioWorkspace(MaterialSupport materials) {
        super(new BorderLayout());
        this.materials = materials;
        setName("studio-workspace");
        getAccessibleContext().setAccessibleName("Workspace");
        materials.apply(this, StudioMaterial.SOLID);
    }

    public void setToolbar(JComponent component) {
        toolbar = replace(toolbar, component, BorderLayout.NORTH);
    }

    public void setSidebar(JComponent component) {
        sidebar = replace(sidebar, component, BorderLayout.WEST);
    }

    public void setContent(JComponent component) {
        content = replace(content, component, BorderLayout.CENTER);
    }

    public void setInspector(JComponent component) {
        inspector = replace(inspector, component, BorderLayout.EAST);
    }

    public void setStatusBar(JComponent component) {
        statusBar = replace(statusBar, component, BorderLayout.SOUTH);
    }

    public JComponent toolbar() {
        return toolbar;
    }

    public JComponent sidebar() {
        return sidebar;
    }

    public JComponent content() {
        return content;
    }

    public JComponent inspector() {
        return inspector;
    }

    public JComponent statusBar() {
        return statusBar;
    }

    private JComponent replace(JComponent previous, JComponent next, String constraint) {
        if (previous != null) {
            remove(previous);
        }
        if (next != null) {
            add(next, constraint);
        }
        revalidate();
        repaint();
        return next;
    }
}
