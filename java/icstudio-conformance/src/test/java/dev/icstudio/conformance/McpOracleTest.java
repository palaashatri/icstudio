package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.icstudio.mcp.McpServer;
import dev.icstudio.platform.json.Json;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Replays the recorded JSON-RPC transcript and compares every response line with the Rust gateway. */
class McpOracleTest {
    @TempDir
    Path temporary;

    @Test
    void transcriptMatchesTheRustGatewayLineForLine() throws IOException {
        Path programme = OracleCorpus.copyTree(OracleCorpus.resolve("evidence/programme"), temporary.resolve("programme"));
        Path active = OracleCorpus.projectCopy(OracleCorpus.resolve("project/expected-final.icst"), temporary.resolve("active"));
        var server = new McpServer(programme, active);

        var responses = new ByteArrayOutputStream();
        try (BufferedReader input = new BufferedReader(new StringReader(OracleCorpus.read("mcp/requests.jsonl")))) {
            server.serve(input, new PrintStream(responses, true, StandardCharsets.UTF_8));
        }

        List<String> actual = responses.toString(StandardCharsets.UTF_8).lines().toList();
        List<String> expected = OracleCorpus.read("mcp/responses.jsonl").lines().toList();
        assertEquals(expected.size(), actual.size(), "response count");
        for (int index = 0; index < expected.size(); index++) {
            // The claim is the one deliberate, scoped difference: the Java gateway states the post-migration claim.
            String normalized = actual.get(index).replace(
                    Json.escape(McpServer.CLAIM), Json.escape(OracleCorpus.RUST_CLAIM));
            assertEquals(expected.get(index), normalized, "response " + index);
        }
    }

}
