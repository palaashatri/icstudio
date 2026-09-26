package dev.icstudio.ui.components;

import dev.icstudio.ui.material.MaterialSupport;
import dev.icstudio.ui.material.StudioMaterial;
import dev.icstudio.ui.theme.StudioTokens;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Reusable contextual inspector container. */
public final class StudioInspector extends JPanel {
    private final JPanel body = new JPanel(new BorderLayout());

    public StudioInspector() {
        this(MaterialSupport.solidFallback());
    }

    public StudioInspector(MaterialSupport materials) {
        super(new BorderLayout());
        setName("studio-inspector");
        getAccessibleContext().setAccessibleName("Inspector");
        setPreferredSize(new Dimension(StudioTokens.INSPECTOR_WIDTH, 1));
        setMinimumSize(new Dimension(200, 1));
        materials.apply(this, StudioMaterial.INSPECTOR);
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
