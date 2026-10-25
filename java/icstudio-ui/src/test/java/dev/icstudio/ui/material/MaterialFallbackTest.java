package dev.icstudio.ui.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.ui.theme.StudioTokens;
import org.junit.jupiter.api.Test;

class MaterialFallbackTest {
    @Test
    void unsupportedBlurFallsBackToOpaqueWithoutChangingTheRole() {
        var result = MaterialResolver.resolve(StudioMaterial.SIDEBAR, false, false);

        assertEquals(StudioMaterial.SIDEBAR, result.role());
        assertFalse(result.backdropBlur());
        assertEquals(1.0f, result.opacity());
    }

    @Test
    void reducedTransparencyForcesAnOpaqueSurfaceEvenWhenBlurExists() {
        var result = MaterialResolver.resolve(StudioMaterial.TOOLBAR, true, true);

        assertFalse(result.backdropBlur());
        assertEquals(1.0f, result.opacity());
    }

    @Test
    void translucentRolesUseTheirTokenOpacityWhenBlurIsAvailable() {
        var result = MaterialResolver.resolve(StudioMaterial.SIDEBAR, true, false);

        assertTrue(result.backdropBlur());
        assertEquals(StudioTokens.defaultOpacity(StudioMaterial.SIDEBAR), result.opacity());
    }

    @Test
    void engineeringCanvasMaterialIsAlwaysOpaqueByDefault() {
        assertEquals(1.0f, StudioTokens.defaultOpacity(StudioMaterial.CANVAS));
        var result = MaterialResolver.resolve(StudioMaterial.CANVAS, true, false);

        assertFalse(result.backdropBlur());
        assertEquals(1.0f, result.opacity());
    }
}
