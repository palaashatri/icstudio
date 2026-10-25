package dev.icstudio.ui.material;

import dev.icstudio.ui.theme.StudioTokens;

/**
 * Resolves a semantic material to what the host can actually render.
 *
 * <p>Failure or an unavailable capability never blocks launch and never changes the requested role.
 * Engineering canvases stay opaque in every configuration, so schematic, layout, and waveform content
 * keep full legibility regardless of wallpaper or compositor effects.</p>
 */
public final class MaterialResolver {
    private MaterialResolver() {
    }

    public static MaterialResolution resolve(StudioMaterial role, boolean nativeBlurAvailable, boolean reduceTransparency) {
        boolean translucencyAllowed = nativeBlurAvailable && !reduceTransparency && role != StudioMaterial.CANVAS;
        if (!translucencyAllowed) {
            return new MaterialResolution(role, false, 1.0f);
        }
        return new MaterialResolution(role, true, StudioTokens.defaultOpacity(role));
    }
}
