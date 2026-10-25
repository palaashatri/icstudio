package dev.icstudio.worker;

/**
 * Versioned, tab-separated control envelopes exchanged with isolated worker processes.
 *
 * <p>Envelopes carry the project identity and expected revision on every request so a worker can
 * never act on implicit state. Values are escaped so tabs and newlines cannot forge fields.</p>
 */
public final class RpcEnvelope {
    public static final int SCHEMA_VERSION = 1;

    private RpcEnvelope() {
    }

    /** A worker request addressed to one project revision. */
    public record Request(
            String requestId,
            String projectId,
            long expectedRevision,
            String command,
            String payload) {
        public Request {
            validateIdentifier("request_id", requestId);
            validateIdentifier("project_id", projectId);
            validateIdentifier("command", command);
        }

        public String encode() {
            return "request\t" + SCHEMA_VERSION + "\t"
                    + escape(requestId) + "\t"
                    + escape(projectId) + "\t"
                    + Long.toUnsignedString(expectedRevision) + "\t"
                    + escape(command) + "\t"
                    + escape(payload);
        }

        public static Request decode(String input) {
            String[] fields = stripLineEnd(input).split("\t", -1);
            if (fields.length != 7 || !"request".equals(fields[0])) {
                throw new IllegalArgumentException("invalid worker request envelope");
            }
            checkVersion(fields[1]);
            var request = new Request(
                    unescape(fields[2]),
                    unescape(fields[3]),
                    parseRevision(fields[4]),
                    unescape(fields[5]),
                    unescape(fields[6]));
            return request;
        }
    }

    /** A worker response correlated to a request by identifier. */
    public record Response(String requestId, boolean success, String payload) {
        public Response {
            validateIdentifier("request_id", requestId);
        }

        public String encode() {
            return "response\t" + SCHEMA_VERSION + "\t"
                    + escape(requestId) + "\t"
                    + (success ? "ok" : "error") + "\t"
                    + escape(payload);
        }

        public static Response decode(String input) {
            String[] fields = stripLineEnd(input).split("\t", -1);
            if (fields.length != 5 || !"response".equals(fields[0])) {
                throw new IllegalArgumentException("invalid worker response envelope");
            }
            checkVersion(fields[1]);
            boolean success = switch (fields[3]) {
                case "ok" -> true;
                case "error" -> false;
                default -> throw new IllegalArgumentException("invalid worker response status '" + fields[3] + "'");
            };
            return new Response(unescape(fields[2]), success, unescape(fields[4]));
        }
    }

    static void validateIdentifier(String kind, String value) {
        if (value == null || value.isEmpty() || value.length() > 256
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(kind + " must be 1 to 256 printable characters");
        }
    }

    private static void checkVersion(String value) {
        long version;
        try {
            version = Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid RPC schema version '" + value + "': " + error.getMessage(), error);
        }
        if (version != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "unsupported RPC schema version " + version + "; expected " + SCHEMA_VERSION);
        }
    }

    private static long parseRevision(String value) {
        try {
            return Long.parseUnsignedLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid expected revision '" + value + "': " + error.getMessage(), error);
        }
    }

    private static String stripLineEnd(String input) {
        int end = input.length();
        while (end > 0 && (input.charAt(end - 1) == '\r' || input.charAt(end - 1) == '\n')) {
            end--;
        }
        return input.substring(0, end);
    }

    static String escape(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> output.append("\\\\");
                case '\t' -> output.append("\\t");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                default -> output.append(character);
            }
        }
        return output.toString();
    }

    static String unescape(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                output.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IllegalArgumentException("unterminated RPC field escape");
            }
            switch (value.charAt(index)) {
                case '\\' -> output.append('\\');
                case 't' -> output.append('\t');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                default -> throw new IllegalArgumentException(
                        "unsupported RPC field escape '\\" + value.charAt(index) + "'");
            }
        }
        return output.toString();
    }
}
