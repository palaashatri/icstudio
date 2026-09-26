package dev.icstudio.ui.components;

import dev.icstudio.ui.material.MaterialSupport;
import dev.icstudio.ui.material.StudioMaterial;
import dev.icstudio.ui.theme.StudioTokens;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Compact command surface for document and workspace actions. */
public final class StudioToolbar extends JPanel {
    public StudioToolbar() {
        this(MaterialSupport.solidFallback());
    }

    public StudioToolbar(MaterialSupport materials) {
        super(new FlowLayout(FlowLayout.LEADING, StudioTokens.SPACE_2, StudioTokens.SPACE_1));
        setName("studio-toolbar");
        getAccessibleContext().setAccessibleName("Toolbar");
        setPreferredSize(new Dimension(1, StudioTokens.TOOLBAR_HEIGHT));
        materials.apply(this, StudioMaterial.TOOLBAR);
    }

    public void addItem(JComponent component) {
        add(component);
    }
}
