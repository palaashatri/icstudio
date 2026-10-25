package dev.icstudio.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.core.ObjectId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExactGeometryTest {
    @Test
    void rectangleArithmeticRejectsOverflow() {
        var rect = new Rect(Long.MAX_VALUE - 1, 0, Long.MAX_VALUE, 1);
        assertThrows(ArithmeticException.class, () -> rect.translated(2, 0));
    }

    @Test
    void touchingRectanglesIntersectAndSeparatedOnesDoNot() {
        var original = new Rect(-10, -5, 10, 5);
        assertTrue(original.intersects(new Rect(10, 5, 20, 10)));
        assertFalse(original.intersects(new Rect(11, 6, 20, 10)));
    }

    @Test
    void invalidRectanglesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Rect(2, 0, 1, 1));
    }

    @Test
    void indexReturnsIdentityOrderedMatches() {
        var shapes = List.of(
                new Shape(new ShapeId(new ObjectId(0, 9)), new Rect(20, 20, 30, 30)),
                new Shape(new ShapeId(new ObjectId(0, 2)), new Rect(0, 0, 10, 10)),
                new Shape(new ShapeId(new ObjectId(0, 7)), new Rect(5, 5, 15, 15)));
        var matches = ReferenceSpatialIndex.build(shapes).query(new Rect(8, 8, 25, 25));

        assertEquals(List.of(2L, 7L, 9L), matches.stream().map(shape -> shape.id().value().low()).toList());
    }
}
