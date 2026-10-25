package dev.icstudio.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Stdio entry point for the MCP gateway; configured by the ICSTUDIO_* environment variables. */
public final class McpMain {
    private McpMain() {
    }

    public static void main(String[] args) throws IOException {
        var server = new McpServer(
                McpServer.projectRootFromEnvironment(System.getenv()),
                McpServer.activeProjectFromEnvironment(System.getenv()));
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        server.serve(input, output);
    }
}
