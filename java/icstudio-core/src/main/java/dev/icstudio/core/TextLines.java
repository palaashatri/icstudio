package dev.icstudio.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Text helpers that reproduce the Rust M1 oracle's string semantics exactly.
 *
 * <p>Rust's {@code lines}, {@code trim}, {@code split_whitespace} and {@code char::is_whitespace}
 * follow Unicode White_Space, which differs from {@link Character#isWhitespace(int)}; the M1
 * parsers depend on these exact boundaries.</p>
 */
public final class TextLines {
    private TextLines() {
    }

    /** Mirrors {@code str::lines}: splits on LF, removes one CR before each LF, and drops a final empty line. */
    public static List<String> lines(String input) {
        var lines = new ArrayList<String>();
        int start = 0;
        while (start < input.length()) {
            int end = input.indexOf('\n', start);
            if (end < 0) {
                lines.add(stripCarriageReturn(input.substring(start)));
                break;
            }
            lines.add(stripCarriageReturn(input.substring(start, end)));
            start = end + 1;
        }
        return lines;
    }

    private static String stripCarriageReturn(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    /** Unicode White_Space membership, identical to Rust's {@code char::is_whitespace}. */
    public static boolean isWhitespace(int codePoint) {
        return (codePoint >= 0x09 && codePoint <= 0x0D)
                || codePoint == 0x20
                || codePoint == 0x85
                || codePoint == 0xA0
                || codePoint == 0x1680
                || (codePoint >= 0x2000 && codePoint <= 0x200A)
                || codePoint == 0x2028
                || codePoint == 0x2029
                || codePoint == 0x202F
                || codePoint == 0x205F
                || codePoint == 0x3000;
    }

    public static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start) {
            int previous = value.codePointBefore(end);
            if (!isWhitespace(previous)) {
                break;
            }
            end -= Character.charCount(previous);
        }
        return value.substring(start, end);
    }

    /** Mirrors {@code str::split_whitespace}: runs of Unicode White_Space separate tokens. */
    public static List<String> splitWhitespace(String value) {
        var tokens = new ArrayList<String>();
        int index = 0;
        int tokenStart = -1;
        while (index < value.length()) {
            int codePoint = value.codePointAt(index);
            int width = Character.charCount(codePoint);
            if (isWhitespace(codePoint)) {
                if (tokenStart >= 0) {
                    tokens.add(value.substring(tokenStart, index));
                    tokenStart = -1;
                }
            } else if (tokenStart < 0) {
                tokenStart = index;
            }
            index += width;
        }
        if (tokenStart >= 0) {
            tokens.add(value.substring(tokenStart));
        }
        return tokens;
    }

    /** ASCII-only case-insensitive comparison, matching Rust's {@code eq_ignore_ascii_case}. */
    public static boolean equalsIgnoreAsciiCase(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        for (int index = 0; index < left.length(); index++) {
            if (asciiLower(left.charAt(index)) != asciiLower(right.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public static char asciiUpper(char value) {
        return value >= 'a' && value <= 'z' ? (char) (value - ('a' - 'A')) : value;
    }

    private static char asciiLower(char value) {
        return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
    }

    /** True when no code point is an ISO control character, matching Rust's {@code char::is_control}. */
    public static boolean hasControl(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }
}
