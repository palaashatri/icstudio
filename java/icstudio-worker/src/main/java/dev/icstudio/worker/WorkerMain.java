package dev.icstudio.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Isolated worker process entry point.
 *
 * <p>The worker reads one request envelope per line from standard input and writes one response per
 * line to standard output. A malformed envelope terminates the worker with status 64. A simulated
 * crash halts the process without cleanup, which the platform must survive without corruption.</p>
 */
public final class WorkerMain {
    static final int EXIT_MALFORMED_REQUEST = 64;
    static final int EXIT_CRASH = 134;

    private WorkerMain() {
    }

    public static void main(String[] args) throws IOException {
        var stdout = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try {
            run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), stdout);
        } catch (IllegalArgumentException error) {
            System.err.println("icstudio-worker: " + error.getMessage());
            System.exit(EXIT_MALFORMED_REQUEST);
        }
    }

    static void run(BufferedReader input, PrintStream output) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            var request = RpcEnvelope.Request.decode(line);
            switch (request.command()) {
                case "worker.ping" -> output.println(new RpcEnvelope.Response(
                        request.requestId(),
                        true,
                        "project=" + request.projectId()
                                + " revision=" + Long.toUnsignedString(request.expectedRevision())
                                + " payload=" + request.payload()).encode());
                case "worker.crash" -> Runtime.getRuntime().halt(EXIT_CRASH);
                default -> output.println(new RpcEnvelope.Response(
                        request.requestId(),
                        false,
                        "unknown worker command '" + request.command() + "'").encode());
            }
        }
    }
}
