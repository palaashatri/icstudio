package dev.icstudio.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic reference spatial index.
 *
 * <p>Shapes are ordered by minimum X and filtered by exact rectangle intersection. Results are
 * returned in stable identity order. This is the auditable baseline that the accepted Rust M1
 * kernel also used; a faster index may replace it only with identical query semantics.</p>
 */
public final class ReferenceSpatialIndex {
    private static final Comparator<Shape> BY_MIN_X_THEN_IDENTITY =
            Comparator.comparingLong((Shape shape) -> shape.bounds().minX())
                    .thenComparingLong(shape -> shape.bounds().minY())
                    .thenComparing(Shape::id);

    private final List<Shape> shapes;
    private final Shape[] byMinX;

    private ReferenceSpatialIndex(List<Shape> shapes, Shape[] byMinX) {
        this.shapes = shapes;
        this.byMinX = byMinX;
    }

    public static ReferenceSpatialIndex build(List<Shape> shapes) {
        var copy = List.copyOf(shapes);
        var sorted = copy.toArray(new Shape[0]);
        Arrays.sort(sorted, BY_MIN_X_THEN_IDENTITY);
        return new ReferenceSpatialIndex(copy, sorted);
    }

    public int size() {
        return shapes.size();
    }

    public List<Shape> shapes() {
        return shapes;
    }

    public List<Shape> query(Rect area) {
        int upper = firstIndexWithMinXAbove(area.maxX());
        var matches = new ArrayList<Shape>();
        for (int index = 0; index < upper; index++) {
            Shape shape = byMinX[index];
            if (shape.bounds().intersects(area)) {
                matches.add(shape);
            }
        }
        matches.sort(Comparator.comparing(Shape::id));
        return List.copyOf(matches);
    }

    private int firstIndexWithMinXAbove(long limit) {
        int low = 0;
        int high = byMinX.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (byMinX[middle].bounds().minX() <= limit) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }
}
