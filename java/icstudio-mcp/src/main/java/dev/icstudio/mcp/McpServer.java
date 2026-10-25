package dev.icstudio.mcp;

import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.core.TextLines;
import dev.icstudio.platform.evidence.ProgrammeEvidence;
import dev.icstudio.platform.json.Json;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Model Context Protocol stdio gateway over the same project services as the CLI and desktop.
 *
 * <p>The gateway is read-only for M1. It never parses project files itself: every project read goes
 * through {@link TransactionalProjectService}, the same path the CLI uses.</p>
 */
public final class McpServer {
    /** Scoped claim reported to clients; it deliberately does not assert solver or editor capability. */
    public static final String CLAIM = "Java 25 authoritative stack; score credit follows accepted checkpoints only";

    private static final String TOOLS = json("{'tools':["
            + "{'name':'capability.report','title':'Report ICStudio capabilities',"
            + "'description':'Return the evidence-backed capability matrix and conservative truth score.',"
            + "'inputSchema':{'type':'object','properties':{},'additionalProperties':false},"
            + "'annotations':{'readOnlyHint':true,'destructiveHint':false,'idempotentHint':true,'openWorldHint':false}},"
            + "{'name':'project.inspect','title':'Inspect active ICStudio project',"
            + "'description':'Return the same authoritative project and revision summary exposed by the CLI.',"
            + "'inputSchema':{'type':'object','properties':{},'additionalProperties':false},"
            + "'annotations':{'readOnlyHint':true,'destructiveHint':false,'idempotentHint':true,'openWorldHint':false}}"
            + "]}");

    private static final String PROMPTS = json("{'prompts':["
            + "{'name':'icstudio.m0.status','title':'Review ICStudio programme status',"
            + "'description':'Evaluate capability evidence without overstating implementation.','arguments':[]},"
            + "{'name':'icstudio.project.review','title':'Review active project hierarchy',"
            + "'description':'Inspect the bounded revision-addressed project summary.','arguments':[]}"
            + "]}");

    private static final String PROMPT_STATUS = json("{'description':'Evidence-first milestone review',"
            + "'messages':[{'role':'user','content':{'type':'text','text':'Read icstudio://status, call "
            + "capability.report, identify unverified claims, and report the truth score. Do not infer solver or "
            + "editor functionality that is not supported by evidence.'}}]}");

    private static final String PROMPT_PROJECT = json("{'description':'Revision-safe project review',"
            + "'messages':[{'role':'user','content':{'type':'text','text':'Call project.inspect and report the "
            + "active project ID, revision, and hierarchy counts. Treat the returned revision as immutable context "
            + "and do not request mutations.'}}]}");

    private final Path root;
    private final Path activeProject;

    public McpServer(Path root, Path activeProject) {
        this.root = root;
        this.activeProject = activeProject;
    }

    public static Path projectRootFromEnvironment(Map<String, String> environment) {
        String value = environment.get("ICSTUDIO_PROJECT_ROOT");
        return value == null ? Path.of(".") : Path.of(value);
    }

    public static Path activeProjectFromEnvironment(Map<String, String> environment) {
        String value = environment.get("ICSTUDIO_ACTIVE_PROJECT");
        return value == null ? null : Path.of(value);
    }

    /** Serves requests until the input ends. Notifications produce no response. */
    public void serve(BufferedReader input, PrintStream output) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            if (TextLines.trim(line).isEmpty()) {
                continue;
            }
            JsonRpcRequest request;
            try {
                request = JsonRpcRequest.parse(line);
            } catch (IllegalArgumentException error) {
                emit(output, errorResponse("null", -32600, error.getMessage()));
                continue;
            }
            if (request.id() == null) {
                continue;
            }
            emit(output, respond(request.id(), request));
        }
    }

    String respond(String id, JsonRpcRequest request) {
        return switch (request.method()) {
            case "initialize" -> initialize(id, request.params());
            case "ping" -> success(id, "{}");
            case "resources/list" -> resourcesList(id);
            case "resources/read" -> resourcesRead(id, request.params());
            case "tools/list" -> success(id, TOOLS);
            case "tools/call" -> toolsCall(id, request.params());
            case "prompts/list" -> success(id, PROMPTS);
            case "prompts/get" -> promptsGet(id, request.params());
            default -> errorResponse(id, -32601, "method not found: " + request.method());
        };
    }

    private String initialize(String id, Map<String, Json.Member> params) {
        String requested;
        try {
            requested = JsonRpcRequest.requiredString(params, "protocolVersion");
        } catch (IllegalArgumentException error) {
            return errorResponse(id, -32602, error.getMessage());
        }
        if (!requested.equals(ProgrammeEvidence.MCP_PROTOCOL_VERSION)) {
            return errorResponse(id, -32602, "unsupported MCP protocol revision " + requested
                    + "; locked revision is " + ProgrammeEvidence.MCP_PROTOCOL_VERSION);
        }
        return success(id, json("{'protocolVersion':'" + ProgrammeEvidence.MCP_PROTOCOL_VERSION + "',"
                + "'capabilities':{'resources':{'subscribe':false,'listChanged':false},"
                + "'tools':{'listChanged':false},'prompts':{'listChanged':false},'logging':{}},"
                + "'serverInfo':{'name':'icstudio-mcp','version':'0.1.0'},"
                + "'instructions':'M1 read-only project gateway. Project mutations and solver execution are not exposed.'}"));
    }

    private String resourcesList(String id) {
        String projectResource = "";
        if (activeProject != null) {
            try {
                long revision = TransactionalProjectService.open(activeProject).snapshot().revision().value();
                projectResource = ",{\"uri\":\"icstudio://project/revision/" + Long.toUnsignedString(revision)
                        + "\",\"name\":\"Active ICStudio project\",\"description\":\"Authoritative project hierarchy summary at revision "
                        + Long.toUnsignedString(revision) + "\",\"mimeType\":\"application/json\"}";
            } catch (IOException | RuntimeException error) {
                projectResource = "";
            }
        }
        return success(id, "{\"resources\":[{\"uri\":\"icstudio://status\",\"name\":\"ICStudio programme status\","
                + "\"description\":\"Evidence-backed capability and truth state\",\"mimeType\":\"application/json\"}"
                + projectResource + "]}");
    }

    private String resourcesRead(String id, Map<String, Json.Member> params) {
        String uri;
        try {
            uri = JsonRpcRequest.requiredString(params, "uri");
        } catch (IllegalArgumentException error) {
            return errorResponse(id, -32602, error.getMessage());
        }
        if (uri.equals("icstudio://status")) {
            return programmeStatus(id);
        }
        if (uri.equals("icstudio://project") || uri.startsWith("icstudio://project/revision/")) {
            return projectResource(id, uri);
        }
        return errorResponse(id, -32002, "unknown resource: " + uri);
    }

    private String programmeStatus(String id) {
        try {
            String capabilities = Files.readString(root.resolve(ProgrammeEvidence.CAPABILITIES_PATH), StandardCharsets.UTF_8);
            String truth = Files.readString(root.resolve(ProgrammeEvidence.TRUTH_PATH), StandardCharsets.UTF_8);
            String body = "{\"capabilities\":" + capabilities.strip() + ",\"truth\":" + truth.strip() + "}";
            return textResource(id, "icstudio://status", body);
        } catch (IOException error) {
            return errorResponse(id, -32001, error.getMessage());
        }
    }

    private String projectResource(String id, String uri) {
        if (activeProject == null) {
            return errorResponse(id, -32003, "ICSTUDIO_ACTIVE_PROJECT is not configured");
        }
        TransactionalProjectService service;
        try {
            service = TransactionalProjectService.open(activeProject);
        } catch (IOException | RuntimeException error) {
            return errorResponse(id, -32001, error.getMessage());
        }
        long revision = service.snapshot().revision().value();
        String prefix = "icstudio://project/revision/";
        if (uri.startsWith(prefix)) {
            String requested = uri.substring(prefix.length());
            long requestedRevision;
            try {
                requestedRevision = Long.parseUnsignedLong(requested);
            } catch (NumberFormatException error) {
                return errorResponse(id, -32602, "invalid project revision '" + requested + "': " + error.getMessage());
            }
            if (requestedRevision != revision) {
                return errorResponse(id, -32004, "project revision conflict: requested "
                        + Long.toUnsignedString(requestedRevision) + ", current " + Long.toUnsignedString(revision));
            }
        }
        return textResource(id, prefix + Long.toUnsignedString(revision), service.snapshot().summaryJson());
    }

    private String toolsCall(String id, Map<String, Json.Member> params) {
        String name;
        try {
            name = JsonRpcRequest.requiredString(params, "name");
        } catch (IllegalArgumentException error) {
            return errorResponse(id, -32602, error.getMessage());
        }
        return switch (name) {
            case "capability.report" -> capabilityTool(id);
            case "project.inspect" -> projectInspectTool(id);
            default -> errorResponse(id, -32602, "unknown tool: " + name);
        };
    }

    private String capabilityTool(String id) {
        try {
            String report = ProgrammeEvidence.capabilityReportMarkdown(root);
            double score = ProgrammeEvidence.truthScore(root);
            return success(id, "{\"content\":[{\"type\":\"text\",\"text\":\"" + Json.escape(report) + "\"}],"
                    + "\"structuredContent\":{\"truthScore\":" + String.format(Locale.ROOT, "%.2f", score)
                    + ",\"maximumScore\":100,\"milestone\":\"M1\",\"claim\":\"" + Json.escape(CLAIM) + "\"},"
                    + "\"isError\":false}");
        } catch (IOException | RuntimeException error) {
            return errorResponse(id, -32001, error.getMessage());
        }
    }

    private String projectInspectTool(String id) {
        if (activeProject == null) {
            return errorResponse(id, -32003, "ICSTUDIO_ACTIVE_PROJECT is not configured");
        }
        try {
            String summary = TransactionalProjectService.open(activeProject).snapshot().summaryJson();
            return success(id, "{\"content\":[{\"type\":\"text\",\"text\":\"" + Json.escape(summary) + "\"}],"
                    + "\"structuredContent\":" + summary + ",\"isError\":false}");
        } catch (IOException | RuntimeException error) {
            return errorResponse(id, -32001, error.getMessage());
        }
    }

    private String promptsGet(String id, Map<String, Json.Member> params) {
        String name;
        try {
            name = JsonRpcRequest.requiredString(params, "name");
        } catch (IllegalArgumentException error) {
            return errorResponse(id, -32602, error.getMessage());
        }
        return switch (name) {
            case "icstudio.m0.status" -> success(id, PROMPT_STATUS);
            case "icstudio.project.review" -> success(id, PROMPT_PROJECT);
            default -> errorResponse(id, -32602, "unknown prompt: " + name);
        };
    }

    private static String textResource(String id, String uri, String body) {
        return success(id, "{\"contents\":[{\"uri\":\"" + Json.escape(uri)
                + "\",\"mimeType\":\"application/json\",\"text\":\"" + Json.escape(body) + "\"}]}");
    }

    static String success(String id, String result) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + result + "}";
    }

    static String errorResponse(String id, int code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":{\"code\":" + code
                + ",\"message\":\"" + Json.escape(message) + "\"}}";
    }

    private static void emit(PrintStream output, String response) {
        output.print(response + "\n");
        output.flush();
    }

    private static String json(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }
}
