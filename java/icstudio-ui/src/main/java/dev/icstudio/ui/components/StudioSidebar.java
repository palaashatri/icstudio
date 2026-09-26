package dev.icstudio.ui.components;

import dev.icstudio.ui.material.MaterialSupport;
import dev.icstudio.ui.material.StudioMaterial;
import dev.icstudio.ui.theme.StudioTokens;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Reusable left-side workspace container. */
public final class StudioSidebar extends JPanel {
    private final JPanel body = new JPanel(new BorderLayout());

    public StudioSidebar() {
        this(MaterialSupport.solidFallback());
    }

    public StudioSidebar(MaterialSupport materials) {
        super(new BorderLayout());
        setName("studio-sidebar");
        getAccessibleContext().setAccessibleName("Sidebar");
        setPreferredSize(new Dimension(StudioTokens.SIDEBAR_WIDTH, 1));
        setMinimumSize(new Dimension(160, 1));
        materials.apply(this, StudioMaterial.SIDEBAR);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    public void setContent(JComponent content) {
        body.removeAll();
        if (content != null) {
            body.add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
