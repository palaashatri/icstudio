package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.cli.IcStudioCli;
import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.mcp.McpServer;
import dev.icstudio.platform.json.Json;
import dev.icstudio.ui.workspace.ProjectWorkbench;
import java.util.Map;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The CLI and MCP gateway must expose identical project state and revision for one project. */
class SurfaceEquivalenceTest {
    @TempDir
    Path temporary;

    @Test
    void cliAndMcpExposeTheSameSnapshot() throws IOException {
        Path project = OracleCorpus.projectCopy(OracleCorpus.resolve("project/expected-final.icst"), temporary.resolve("p"));
        Path programme = OracleCorpus.copyTree(OracleCorpus.resolve("evidence/programme"), temporary.resolve("programme"));
        String expected = TransactionalProjectService.open(project).snapshot().summaryJson();

        var cliOut = new ByteArrayOutputStream();
        assertEquals(0, IcStudioCli.run(
                new String[] {"project", "show", "--path", project.toString()},
                new PrintStream(cliOut, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)));
        assertEquals(expected, cliOut.toString(StandardCharsets.UTF_8).strip());

        var server = new McpServer(programme, project);
        var mcpOut = new ByteArrayOutputStream();
        server.serve(
                new BufferedReader(new StringReader(
                        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"project.inspect\",\"arguments\":{}}}\n")),
                new PrintStream(mcpOut, true, StandardCharsets.UTF_8));
        String response = mcpOut.toString(StandardCharsets.UTF_8).strip();
        assertTrue(response.contains("\"structuredContent\":" + expected + ","), response);
        assertTrue(response.contains("\"text\":\"" + Json.escape(expected) + "\""), response);
    }

    @Test
    void swingWorkbenchDisplaysTheSameSnapshotAsTheSummary() throws IOException {
        Path project = OracleCorpus.projectCopy(OracleCorpus.resolve("project/expected-final.icst"), temporary.resolve("swing"));
        var snapshot = TransactionalProjectService.open(project).snapshot();
        var summary = (Map<?, ?>) Json.parse(snapshot.summaryJson());

        var workbench = new ProjectWorkbench();
        workbench.bind(snapshot);
        var shown = workbench.inspector().displayedProperties();

        assertEquals(summary.get("name").toString(), shown.get("Name"));
        assertEquals(summary.get("projectId").toString(), shown.get("Project ID"));
        assertEquals(summary.get("revision").toString(), shown.get("Revision"));
        assertEquals(summary.get("libraries").toString(), shown.get("Libraries"));
        assertEquals(summary.get("cells").toString(), shown.get("Cells"));
        assertEquals(summary.get("views").toString(), shown.get("Views"));
    }
}
