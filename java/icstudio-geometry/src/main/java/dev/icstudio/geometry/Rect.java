package dev.icstudio.geometry;

/**
 * Exact axis-aligned rectangle in signed 64-bit database units.
 *
 * <p>Translation is overflow-checked: an overflowing transform is rejected rather than wrapped,
 * so authoritative coordinates never change silently.</p>
 */
public record Rect(long minX, long minY, long maxX, long maxY) {
    public Rect {
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException(
                    String.format("invalid rectangle [%d,%d]..[%d,%d]", minX, minY, maxX, maxY));
        }
    }

    /** Inclusive intersection, so rectangles that only touch along an edge intersect. */
    public boolean intersects(Rect other) {
        return minX <= other.maxX
                && maxX >= other.minX
                && minY <= other.maxY
                && maxY >= other.minY;
    }

    public Rect translated(long deltaX, long deltaY) {
        long nextMinX;
        long nextMaxX;
        long nextMinY;
        long nextMaxY;
        try {
            nextMinX = Math.addExact(minX, deltaX);
            nextMaxX = Math.addExact(maxX, deltaX);
        } catch (ArithmeticException overflow) {
            throw new ArithmeticException("rectangle X translation overflow");
        }
        try {
            nextMinY = Math.addExact(minY, deltaY);
            nextMaxY = Math.addExact(maxY, deltaY);
        } catch (ArithmeticException overflow) {
            throw new ArithmeticException("rectangle Y translation overflow");
        }
        return new Rect(nextMinX, nextMinY, nextMaxX, nextMaxY);
    }
}
