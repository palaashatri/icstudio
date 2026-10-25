package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.geometry.MillionShapeBaseline;
import dev.icstudio.geometry.Rect;
import dev.icstudio.geometry.ReferenceSpatialIndex;
import dev.icstudio.geometry.Shape;
import dev.icstudio.geometry.ShapeId;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Compares exact geometry and the one-million-shape query with the recorded Rust baseline. */
class GeometryOracleTest {
    @Test
    void rectangleRejectionsMatchTheRustMessages() {
        var invalid = assertThrows(IllegalArgumentException.class, () -> new Rect(2, 0, 1, 1));
        assertEquals("invalid rectangle [2,0]..[1,1]", invalid.getMessage());

        var overflow = assertThrows(ArithmeticException.class,
                () -> new Rect(Long.MAX_VALUE, 0, Long.MAX_VALUE, 1).translated(1, 0));
        assertEquals("rectangle X translation overflow", overflow.getMessage());
        assertEquals(new Rect(10, 25, 30, 35), new Rect(-10, -5, 10, 5).translated(20, 30));
    }

    @Test
    void oneMillionShapeQueryMatchesTheRustBaseline() throws IOException {
        String recorded = OracleCorpus.read("geometry/million.txt");
        var index = ReferenceSpatialIndex.build(MillionShapeBaseline.shapes());
        var matches = index.query(MillionShapeBaseline.queryArea());

        assertEquals(fieldValue(recorded, "shape_count"), Integer.toString(index.size()));
        assertEquals(fieldValue(recorded, "match_count"), Integer.toString(matches.size()));
        assertEquals(fieldValue(recorded, "match_ids"), matches.stream()
                .map(Shape::id)
                .map(id -> Long.toUnsignedString(id.value().low()))
                .collect(Collectors.joining(",")));
        assertEquals(fieldValue(recorded, "baseline_match_count"), Integer.toString(matches.size()));
    }

    @Test
    void touchingShapesMatchAndSeparateShapesDoNotInIdentityOrder() {
        var shapes = List.of(
                new Shape(ShapeId.of(2), new Rect(20, 0, 30, 10)),
                new Shape(ShapeId.of(0), new Rect(0, 0, 10, 10)),
                new Shape(ShapeId.of(1), new Rect(10, 0, 20, 10)));
        var matches = ReferenceSpatialIndex.build(shapes).query(new Rect(10, 0, 19, 10));

        assertEquals(List.of(0L, 1L), matches.stream().map(shape -> shape.id().value().low()).toList());
    }

    private static String fieldValue(String recorded, String key) {
        for (String line : recorded.split("\n")) {
            if (line.startsWith(key + " ")) {
                return line.substring(key.length() + 1);
            }
        }
        throw new IllegalStateException("missing " + key);
    }
}
