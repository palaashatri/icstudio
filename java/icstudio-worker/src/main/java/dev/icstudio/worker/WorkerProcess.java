package dev.icstudio.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts a worker in its own JVM and exchanges one request for one response.
 *
 * <p>The worker has no access to project state: every request carries the project identity and
 * expected revision, and the parent keeps authoritative state outside the child process.</p>
 */
public final class WorkerProcess {
    private static final long TIMEOUT_SECONDS = 60;

    private WorkerProcess() {
    }

    /** Runs the request in a fresh worker JVM. The process exit status is returned, never thrown. */
    public static Outcome exchange(RpcEnvelope.Request request) throws IOException, InterruptedException {
        var command = javaCommand();
        var process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (Writer writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(request.encode());
            writer.write('\n');
        }
        String line;
        RpcEnvelope.Response response = null;
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    response = RpcEnvelope.Response.decode(line);
                    break;
                }
            }
        }
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("worker did not exit within " + TIMEOUT_SECONDS + " seconds");
        }
        return new Outcome(process.exitValue(), response);
    }

    /** The worker exit status and the response, if the worker produced one before exiting. */
    public record Outcome(int exitCode, RpcEnvelope.Response response) {
        public boolean crashed() {
            return response == null;
        }
    }

    public static List<String> javaCommand() {
        String executable = ProcessHandle.current().info().command()
                .orElseGet(() -> Path.of(System.getProperty("java.home"), "bin", "java").toString());
        var command = new ArrayList<String>();
        command.add(executable);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(WorkerMain.class.getName());
        return List.copyOf(command);
    }
}
