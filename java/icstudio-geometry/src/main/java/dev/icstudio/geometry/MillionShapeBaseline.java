package dev.icstudio.geometry;

import java.util.ArrayList;
import java.util.List;

/** Deterministic one-million-shape reference corpus shared by the baseline test and evidence. */
public final class MillionShapeBaseline {
    public static final int SHAPE_COUNT = 1_000_000;

    private MillionShapeBaseline() {
    }

    public static List<Shape> shapes() {
        var shapes = new ArrayList<Shape>(SHAPE_COUNT);
        for (int index = 0; index < SHAPE_COUNT; index++) {
            long x = Math.multiplyExact((long) index, 4L);
            shapes.add(new Shape(ShapeId.of(index), new Rect(x, 0, x + 2, 2)));
        }
        return shapes;
    }

    public static Rect queryArea() {
        return new Rect(400, 0, 438, 2);
    }
}
