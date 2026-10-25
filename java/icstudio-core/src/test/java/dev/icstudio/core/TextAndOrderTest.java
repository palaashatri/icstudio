package dev.icstudio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextAndOrderTest {
    @Test
    void utf8OrderMatchesByteOrderForSupplementaryCharacters() {
        // U+FFFD (BMP, UTF-8 EF BF BD) sorts before U+1F600 (UTF-8 F0 9F 98 80), but UTF-16 would reverse them.
        var names = new java.util.ArrayList<>(List.of("😀", "�"));
        names.sort(Utf8Order.COMPARATOR);
        assertEquals(List.of("�", "😀"), names);
        assertEquals(4, Utf8Order.utf8Length("😀"));
    }

    @Test
    void linesMirrorRustSemantics() {
        assertEquals(List.of("a", "", "b"), TextLines.lines("a\n\nb\n"));
        assertEquals(List.of("a", "b"), TextLines.lines("a\r\nb"));
        assertEquals(List.of(), TextLines.lines(""));
    }

    @Test
    void whitespaceUsesUnicodeWhiteSpace() {
        assertTrue(TextLines.isWhitespace(0xA0));
        assertTrue(TextLines.isWhitespace(0x3000));
        assertFalse(TextLines.isWhitespace('x'));
        assertEquals(List.of("R1", "a", "b"), TextLines.splitWhitespace("  R1 a\tb "));
        assertEquals("x", TextLines.trim(" x　"));
    }

    @Test
    void objectIdRoundTripsAsFixedWidthHex() {
        var id = new ObjectId(0x0123456789ABCDEFL, 0x0FEDCBA987654321L);
        assertEquals("0123456789abcdef0fedcba987654321", id.toHex());
        assertEquals(id, ObjectId.parseHex(id.toHex()));
    }

    @Test
    void revisionIncrementRejectsOverflow() {
        var last = new Revision(-1L);
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, last::next);
    }
}
