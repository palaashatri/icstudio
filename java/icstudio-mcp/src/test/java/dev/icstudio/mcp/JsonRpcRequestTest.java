package dev.icstudio.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JsonRpcRequestTest {
    @Test
    void topLevelIdentifiersIgnoreNestedLookalikes() {
        var request = JsonRpcRequest.parse(
                "{\"jsonrpc\":\"2.0\",\"params\":{\"id\":1,\"method\":\"wrong\",\"name\":\"nested\"},\"id\":7,\"method\":\"ping\"}");

        assertEquals("7", request.id());
        assertEquals("ping", request.method());
        assertEquals("wrong", JsonRpcRequest.requiredString(request.params(), "method"));
    }

    @Test
    void notificationsHaveNoIdentifier() {
        assertNull(JsonRpcRequest.parse("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}").id());
    }

    @Test
    void duplicateAndUnknownTopLevelFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonRpcRequest.parse("{\"jsonrpc\":\"2.0\",\"id\":1,\"id\":2,\"method\":\"ping\"}"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> JsonRpcRequest.parse("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"unexpected\":true}"));
        assertEquals("unsupported top-level JSON-RPC field 'unexpected'", error.getMessage());
    }

    @Test
    void stringIdentifiersAreEchoedExactly() {
        assertEquals("\"abc\"", JsonRpcRequest.parse("{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"ping\"}").id());
    }
}
