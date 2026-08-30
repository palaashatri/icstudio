package dev.icstudio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ObjectIdTest {
    @Test
    void roundTripsAsFixedWidthLowercaseHex() {
        var id = new ObjectId(0x0123456789ABCDEFL, 0x0FEDCBA987654321L);

        assertEquals("0123456789abcdef0fedcba987654321", id.toHex());
        assertEquals(id, ObjectId.parseHex(id.toHex()));
    }

    @Test
    void rejectsAnythingOtherThanExactly128BitsOfHex() {
        assertThrows(IllegalArgumentException.class, () -> ObjectId.parseHex("1234"));
        assertThrows(IllegalArgumentException.class,
                () -> ObjectId.parseHex("0123456789abcdef0fedcba98765432z"));
    }
}
