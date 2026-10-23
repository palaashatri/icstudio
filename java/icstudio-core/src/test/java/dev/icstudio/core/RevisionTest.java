package dev.icstudio.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RevisionTest {
    @Test
    void parsesAndFormatsUnsignedValues() {
        assertEquals(Revision.ZERO, Revision.parseUnsigned("0"));

        var max = Revision.parseUnsigned("18446744073709551615");
        assertEquals("18446744073709551615", max.toUnsignedString());
        assertTrue(max.compareTo(new Revision(Long.MAX_VALUE)) > 0);
    }

    @Test
    void incrementIsUnsignedAndRejectsOverflow() {
        assertEquals(new Revision(1L), Revision.ZERO.next());
        assertThrows(ArithmeticException.class, () -> new Revision(-1L).next());
    }

    @Test
    void rejectsSignedOrMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> Revision.parseUnsigned("-1"));
        assertThrows(IllegalArgumentException.class, () -> Revision.parseUnsigned("1.0"));
        assertThrows(IllegalArgumentException.class, () -> Revision.parseUnsigned(""));
    }
}
