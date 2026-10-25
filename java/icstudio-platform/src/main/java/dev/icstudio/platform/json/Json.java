package dev.icstudio.platform.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strict RFC 8259 JSON reader for ICStudio's own evidence and protocol files.
 *
 * <p>Objects keep insertion order. Duplicate object keys are rejected rather than resolved, because
 * an ambiguous key could change which value a gate reads.</p>
 */
public final class Json {
    /** The JSON {@code null} literal, distinct from a missing key. */
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    /** A top-level object member: its exact source text and its parsed value. */
    public record Member(String raw, Object value) {
    }

    private Json() {
    }

    /** Escapes a value for inclusion inside a JSON string literal. */
    public static String escape(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        return output.toString();
    }

    public static Object parse(String text) {
        var parser = new Parser(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("trailing data after JSON value");
        }
        return value;
    }

    /** Parses a top-level object and returns each member with its raw text, in source order. */
    public static Map<String, Member> parseTopLevelMembers(String text) {
        var parser = new Parser(text);
        parser.skipWhitespace();
        if (parser.atEnd() || parser.peek() != '{') {
            throw new IllegalArgumentException("JSON value is not an object");
        }
        parser.position++;
        var members = new LinkedHashMap<String, Member>();
        parser.skipWhitespace();
        if (!parser.atEnd() && parser.peek() == '}') {
            parser.position++;
        } else {
            while (true) {
                parser.skipWhitespace();
                String key = parser.string();
                parser.skipWhitespace();
                parser.expect(':');
                parser.skipWhitespace();
                int start = parser.position;
                Object value = parser.value();
                String raw = text.substring(start, parser.position);
                if (members.putIfAbsent(key, new Member(raw, value)) != null) {
                    throw new IllegalArgumentException("duplicate JSON field '" + key + "'");
                }
                parser.skipWhitespace();
                if (parser.atEnd()) {
                    throw new IllegalArgumentException("JSON object is missing ',' or '}'");
                }
                char separator = parser.peek();
                parser.position++;
                if (separator == '}') {
                    break;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("JSON object is missing ',' or '}'");
                }
            }
        }
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("trailing data after JSON object");
        }
        return members;
    }

    private static final class Parser {
        private final String text;
        int position;

        Parser(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return position >= text.length();
        }

        char peek() {
            return text.charAt(position);
        }

        void expect(char expected) {
            if (atEnd() || peek() != expected) {
                throw new IllegalArgumentException("expected '" + expected + "' in JSON at offset " + position);
            }
            position++;
        }

        void skipWhitespace() {
            while (!atEnd()) {
                char character = peek();
                if (character != ' ' && character != '\t' && character != '\n' && character != '\r') {
                    return;
                }
                position++;
            }
        }

        Object value() {
            skipWhitespace();
            if (atEnd()) {
                throw new IllegalArgumentException("unexpected end of JSON input");
            }
            char character = peek();
            return switch (character) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", NULL);
                default -> {
                    if (character == '-' || (character >= '0' && character <= '9')) {
                        yield number();
                    }
                    throw new IllegalArgumentException("unexpected character in JSON at offset " + position);
                }
            };
        }

        private Map<String, Object> object() {
            position++;
            var members = new LinkedHashMap<String, Object>();
            skipWhitespace();
            if (!atEnd() && peek() == '}') {
                position++;
                return members;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = value();
                if (members.containsKey(key)) {
                    throw new IllegalArgumentException("duplicate JSON field '" + key + "'");
                }
                members.put(key, value);
                skipWhitespace();
                if (atEnd()) {
                    throw new IllegalArgumentException("JSON object is missing ',' or '}'");
                }
                char separator = peek();
                position++;
                if (separator == '}') {
                    return members;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("JSON object is missing ',' or '}'");
                }
            }
        }

        private List<Object> array() {
            position++;
            var items = new ArrayList<Object>();
            skipWhitespace();
            if (!atEnd() && peek() == ']') {
                position++;
                return items;
            }
            while (true) {
                items.add(value());
                skipWhitespace();
                if (atEnd()) {
                    throw new IllegalArgumentException("unterminated JSON array");
                }
                char separator = peek();
                position++;
                if (separator == ']') {
                    return items;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("JSON array is missing ',' or ']'");
                }
            }
        }

        private Object literal(String word, Object value) {
            if (!text.startsWith(word, position)) {
                throw new IllegalArgumentException("invalid JSON literal at offset " + position);
            }
            position += word.length();
            return value;
        }

        private BigDecimal number() {
            int start = position;
            if (peek() == '-') {
                position++;
            }
            if (atEnd() || !isDigit(peek())) {
                throw new IllegalArgumentException("invalid JSON number at offset " + start);
            }
            if (peek() == '0') {
                position++;
            } else {
                while (!atEnd() && isDigit(peek())) {
                    position++;
                }
            }
            if (!atEnd() && peek() == '.') {
                position++;
                if (atEnd() || !isDigit(peek())) {
                    throw new IllegalArgumentException("invalid JSON number at offset " + start);
                }
                while (!atEnd() && isDigit(peek())) {
                    position++;
                }
            }
            if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
                position++;
                if (!atEnd() && (peek() == '+' || peek() == '-')) {
                    position++;
                }
                if (atEnd() || !isDigit(peek())) {
                    throw new IllegalArgumentException("invalid JSON number at offset " + start);
                }
                while (!atEnd() && isDigit(peek())) {
                    position++;
                }
            }
            return new BigDecimal(text.substring(start, position));
        }

        String string() {
            if (atEnd() || peek() != '"') {
                throw new IllegalArgumentException("JSON value is not a string");
            }
            position++;
            var output = new StringBuilder();
            while (!atEnd()) {
                char character = peek();
                position++;
                if (character == '"') {
                    return output.toString();
                }
                if (character < 0x20) {
                    throw new IllegalArgumentException("control character in JSON string");
                }
                if (character != '\\') {
                    output.append(character);
                    continue;
                }
                if (atEnd()) {
                    throw new IllegalArgumentException("unterminated JSON string escape");
                }
                char escaped = peek();
                position++;
                switch (escaped) {
                    case '"' -> output.append('"');
                    case '\\' -> output.append('\\');
                    case '/' -> output.append('/');
                    case 'b' -> output.append('\b');
                    case 'f' -> output.append('\f');
                    case 'n' -> output.append('\n');
                    case 'r' -> output.append('\r');
                    case 't' -> output.append('\t');
                    case 'u' -> output.append(unicodeEscape());
                    default -> throw new IllegalArgumentException("unsupported JSON string escape");
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        private char unicodeEscape() {
            if (position + 4 > text.length()) {
                throw new IllegalArgumentException("incomplete JSON unicode escape");
            }
            String digits = text.substring(position, position + 4);
            for (int index = 0; index < digits.length(); index++) {
                if (Character.digit(digits.charAt(index), 16) < 0) {
                    throw new IllegalArgumentException("invalid JSON unicode escape '" + digits + "'");
                }
            }
            position += 4;
            return (char) Integer.parseInt(digits, 16);
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }
    }
}
