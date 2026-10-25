package dev.icstudio.mcp;

import dev.icstudio.core.TextLines;
import dev.icstudio.platform.json.Json;
import java.util.Map;

/**
 * One parsed JSON-RPC 2.0 request.
 *
 * <p>Only top-level members are read for {@code jsonrpc}, {@code id}, {@code method}, and
 * {@code params}. Nested fields that look like these names are never mistaken for them.</p>
 *
 * @param id the identifier exactly as it appeared in the request, or {@code null} for a notification
 */
public record JsonRpcRequest(String id, String method, Map<String, Json.Member> params) {
    static JsonRpcRequest parse(String line) {
        Map<String, Json.Member> members = Json.parseTopLevelMembers(line);
        for (String key : members.keySet()) {
            if (!key.equals("jsonrpc") && !key.equals("id") && !key.equals("method") && !key.equals("params")) {
                throw new IllegalArgumentException("unsupported top-level JSON-RPC field '" + key + "'");
            }
        }
        String version = requiredString(members, "jsonrpc");
        if (!version.equals("2.0")) {
            throw new IllegalArgumentException("unsupported JSON-RPC version '" + version + "'");
        }
        String method = requiredString(members, "method");
        String id = members.containsKey("id") ? validateId(members.get("id").raw()) : null;
        Map<String, Json.Member> params = members.containsKey("params")
                ? objectMembers(members.get("params"))
                : Map.of();
        return new JsonRpcRequest(id, method, params);
    }

    /** Reads a required string member of a parsed object. */
    static String requiredString(Map<String, Json.Member> object, String key) {
        var member = object.get(key);
        if (member == null) {
            throw new IllegalArgumentException("missing JSON string field '" + key + "'");
        }
        return stringValue(member);
    }

    static String stringValue(Json.Member member) {
        if (member.value() instanceof String value) {
            return value;
        }
        throw new IllegalArgumentException("JSON value is not a string");
    }

    private static Map<String, Json.Member> objectMembers(Json.Member member) {
        if (!member.raw().startsWith("{")) {
            throw new IllegalArgumentException("JSON value is not an object");
        }
        return Json.parseTopLevelMembers(member.raw());
    }

    private static String validateId(String raw) {
        String trimmed = TextLines.trim(raw);
        if (trimmed.startsWith("\"")) {
            return trimmed;
        }
        if (trimmed.equals("null") || trimmed.contains(".") || trimmed.contains("e") || trimmed.contains("E")) {
            throw new IllegalArgumentException("JSON-RPC id must be a string or integer");
        }
        try {
            Long.parseLong(trimmed);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("JSON-RPC id must be a string or integer");
        }
        return trimmed;
    }
}
