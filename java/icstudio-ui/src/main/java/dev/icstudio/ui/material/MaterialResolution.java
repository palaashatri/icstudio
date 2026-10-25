package dev.icstudio.ui.material;

/**
 * The concrete rendering decision for one semantic material.
 *
 * @param role the requested semantic role, which is never changed by fallback
 * @param backdropBlur whether a native backdrop blur is applied
 * @param opacity the surface opacity, where {@code 1.0} is fully opaque
 */
public record MaterialResolution(StudioMaterial role, boolean backdropBlur, float opacity) {
}
