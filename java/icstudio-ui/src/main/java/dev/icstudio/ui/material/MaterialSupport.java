package dev.icstudio.ui.material;

import javax.swing.JComponent;

/**
 * Applies a semantic material to a Swing component without exposing
 * platform-specific blur/vibrancy APIs to product UI code.
 */
@FunctionalInterface
public interface MaterialSupport {
    void apply(JComponent component, StudioMaterial material);

    default boolean supports(StudioMaterial material) {
        return material == StudioMaterial.SOLID || material == StudioMaterial.CANVAS;
    }

    static MaterialSupport solidFallback() {
        return SolidMaterialSupport.INSTANCE;
    }
}
