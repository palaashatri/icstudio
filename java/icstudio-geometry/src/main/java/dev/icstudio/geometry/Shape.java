package dev.icstudio.geometry;

/** A shape identity bound to its exact bounding rectangle. */
public record Shape(ShapeId id, Rect bounds) {
}
