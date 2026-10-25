package dev.icstudio.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RpcEnvelopeTest {
    @Test
    void requestRoundTripPreservesRevisionAndEscapedPayload() {
        var request = new RpcEnvelope.Request("request-1", "0123456789abcdef0123456789abcdef", 42,
                "worker.ping", "line one\nline two\tvalue");
        var decoded = RpcEnvelope.Request.decode(request.encode());

        assertEquals(request, decoded);
        assertEquals(42L, decoded.expectedRevision());
    }

    @Test
    void responseRoundTripPreservesErrorState() {
        var response = new RpcEnvelope.Response("request-2", false, "worker rejected input");
        assertEquals(response, RpcEnvelope.Response.decode(response.encode()));
        assertFalse(RpcEnvelope.Response.decode(response.encode()).success());
    }

    @Test
    void unsupportedVersionsAreRejected() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> RpcEnvelope.Request.decode("request\t999\tr\tp\t0\tworker.ping\t"));
        assertEquals("unsupported RPC schema version 999; expected 1", error.getMessage());
    }
}
