package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.worker.RpcEnvelope;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** Compares worker envelope encoding and decoding with the recorded Rust oracle. */
class RpcOracleTest {
    @Test
    void encodingMatchesTheRustEnvelopes() throws IOException {
        for (String name : OracleCorpus.cases("rpc/encode", "input")) {
            String[] fields = OracleCorpus.read("rpc/encode/" + name + ".input").split("\t", -1);
            if (name.startsWith("response")) {
                var response = new RpcEnvelope.Response(fields[0], fields[1].equals("ok"), fields[2]);
                assertEquals(OracleCorpus.read("rpc/encode/" + name + ".expected"), response.encode(), name);
                continue;
            }
            if (Files.exists(OracleCorpus.resolve("rpc/encode/" + name + ".error"))) {
                var error = assertThrows(IllegalArgumentException.class, () -> new RpcEnvelope.Request(
                        fields[0], fields[1], Long.parseUnsignedLong(fields[2]), fields[3], fields[4]).encode(), name);
                assertEquals(OracleCorpus.read("rpc/encode/" + name + ".error"), error.getMessage(), name);
            } else {
                var request = new RpcEnvelope.Request(
                        fields[0], fields[1], Long.parseUnsignedLong(fields[2]), fields[3], fields[4]);
                assertEquals(OracleCorpus.read("rpc/encode/" + name + ".expected"), request.encode(), name);
            }
        }
    }

    @Test
    void decodingMatchesTheRustOutcome() throws IOException {
        for (String name : OracleCorpus.cases("rpc/decode", "input")) {
            String line = OracleCorpus.read("rpc/decode/" + name + ".input");
            if (Files.exists(OracleCorpus.resolve("rpc/decode/" + name + ".error"))) {
                var error = assertThrows(IllegalArgumentException.class, () -> decodeAndEncode(line), name);
                assertEquals(OracleCorpus.read("rpc/decode/" + name + ".error"), error.getMessage(), name);
            } else {
                assertEquals(OracleCorpus.read("rpc/decode/" + name + ".expected"), decodeAndEncode(line), name);
            }
        }
    }

    private static String decodeAndEncode(String line) {
        return line.startsWith("request\t")
                ? RpcEnvelope.Request.decode(line).encode()
                : RpcEnvelope.Response.decode(line).encode();
    }
}
