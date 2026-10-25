package dev.icstudio.core;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;

/**
 * Orders strings by their UTF-8 byte sequence.
 *
 * <p>The authoritative M1 oracle keys deterministic maps by UTF-8 bytes. UTF-8 preserves
 * code point order, so comparing code points reproduces that ordering exactly, including
 * supplementary characters that UTF-16 ordering would place differently.</p>
 */
public final class Utf8Order {
    public static final Comparator<String> COMPARATOR = Utf8Order::compare;

    private Utf8Order() {
    }

    public static int compare(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            int leftCodePoint = left.codePointAt(leftIndex);
            int rightCodePoint = right.codePointAt(rightIndex);
            if (leftCodePoint != rightCodePoint) {
                return Integer.compare(leftCodePoint, rightCodePoint);
            }
            leftIndex += Character.charCount(leftCodePoint);
            rightIndex += Character.charCount(rightCodePoint);
        }
        return Boolean.compare(leftIndex < left.length(), rightIndex < right.length());
    }

    /** Returns the UTF-8 length in bytes, the unit used by the M1 name and token limits. */
    public static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
