package dev.icstudio.geometry;

import dev.icstudio.core.ObjectId;

/** Stable 128-bit identity of a geometric shape, ordered as an unsigned integer. */
public record ShapeId(ObjectId value) implements Comparable<ShapeId> {
    public static ShapeId of(long index) {
        return new ShapeId(new ObjectId(0L, index));
    }

    @Override
    public int compareTo(ShapeId other) {
        return value.compareTo(other.value);
    }
}
