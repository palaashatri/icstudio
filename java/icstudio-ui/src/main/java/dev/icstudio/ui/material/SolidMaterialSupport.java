package dev.icstudio.ui.material;

import dev.icstudio.ui.theme.StudioTokens;
import java.awt.Color;
import javax.swing.JComponent;

final class SolidMaterialSupport implements MaterialSupport {
    static final SolidMaterialSupport INSTANCE = new SolidMaterialSupport();

    private SolidMaterialSupport() {
    }

    @Override
    public void apply(JComponent component, StudioMaterial material) {
        component.putClientProperty("icstudio.material", material.name());
        component.setOpaque(true);
        component.setBackground(background(material));
    }

    @Override
    public boolean supports(StudioMaterial material) {
        return true;
    }

    private static Color background(StudioMaterial material) {
        return switch (material) {
            case SIDEBAR -> StudioTokens.sidebarBackground();
            case TOOLBAR -> StudioTokens.toolbarBackground();
            case INSPECTOR -> StudioTokens.inspectorBackground();
            case CANVAS -> StudioTokens.canvasBackground();
            case SOLID, POPOVER, MENU, HUD -> StudioTokens.elevatedBackground();
        };
    }
}
