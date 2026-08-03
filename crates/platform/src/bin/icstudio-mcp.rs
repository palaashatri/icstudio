mod jsonrpc;

use icstudio_platform::{
    CAPABILITIES_PATH, MCP_PROTOCOL_VERSION, TRUTH_PATH, capability_report_markdown, escape_json,
    project_root_from_env, read_required, truth_score,
};
use icstudio_project::ProjectStore;
use jsonrpc::{Object, Request};
use std::io::{self, BufRead, Write};
use std::path::{Path, PathBuf};

fn main() {
    let root = project_root_from_env();
    let active_project = std::env::var_os("ICSTUDIO_ACTIVE_PROJECT").map(PathBuf::from);
    let stdin = io::stdin();
    let mut stdout = io::stdout().lock();

    for line in stdin.lock().lines() {
        let line = match line {
            Ok(value) => value,
            Err(error) => {
                eprintln!("icstudio-mcp: failed to read stdin: {error}");
                break;
            }
        };
        if line.trim().is_empty() {
            continue;
        }

        let request = match Request::parse(&line) {
            Ok(value) => value,
            Err(error) => {
                emit(&mut stdout, &error_response("null", -32600, &error));
                continue;
            }
        };
        let Some(id) = request.id.as_deref() else {
            continue;
        };
        let response = match request.method.as_str() {
            "initialize" => initialize_response(id, &request.params),
            "ping" => success(id, "{}"),
            "resources/list" => resources_list(id, active_project.as_deref()),
            "resources/read" => {
                resources_read(id, &request.params, &root, active_project.as_deref())
            }
            "tools/list" => tools_list(id),
            "tools/call" => tools_call(id, &request.params, &root, active_project.as_deref()),
            "prompts/list" => prompts_list(id),
            "prompts/get" => prompts_get(id, &request.params),
            method => error_response(id, -32601, &format!("method not found: {method}")),
        };
        emit(&mut stdout, &response);
    }
}

fn emit(stdout: &mut impl Write, response: &str) {
    if writeln!(stdout, "{response}").is_err() || stdout.flush().is_err() {
        eprintln!("icstudio-mcp: failed to write stdout");
    }
}

fn initialize_response(id: &str, params: &Object) -> String {
    let requested = match params.required_string("protocolVersion") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    if requested != MCP_PROTOCOL_VERSION {
        return error_response(
            id,
            -32602,
            &format!(
                "unsupported MCP protocol revision {requested}; locked revision is {MCP_PROTOCOL_VERSION}"
            ),
        );
    }
    success(
        id,
        &format!(
            "{{\"protocolVersion\":\"{MCP_PROTOCOL_VERSION}\",\"capabilities\":{{\"resources\":{{\"subscribe\":false,\"listChanged\":false}},\"tools\":{{\"listChanged\":false}},\"prompts\":{{\"listChanged\":false}},\"logging\":{{}}}},\"serverInfo\":{{\"name\":\"icstudio-mcp\",\"version\":\"0.1.0\"}},\"instructions\":\"M1 read-only project gateway. Project mutations and solver execution are not exposed.\"}}"
        ),
    )
}

fn resources_list(id: &str, active_project: Option<&Path>) -> String {
    let project_resource = match active_project.and_then(|path| ProjectStore::open(path).ok()) {
        Some(store) => format!(
            ",{{\"uri\":\"icstudio://project/revision/{}\",\"name\":\"Active ICStudio project\",\"description\":\"Authoritative project hierarchy summary at revision {}\",\"mimeType\":\"application/json\"}}",
            store.project().revision,
            store.project().revision
        ),
        None => String::new(),
    };
    success(
        id,
        &format!(
            "{{\"resources\":[{{\"uri\":\"icstudio://status\",\"name\":\"ICStudio programme status\",\"description\":\"Evidence-backed capability and truth state\",\"mimeType\":\"application/json\"}}{project_resource}]}}"
        ),
    )
}

fn resources_read(id: &str, params: &Object, root: &Path, active_project: Option<&Path>) -> String {
    let uri = match params.required_string("uri") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    if uri == "icstudio://status" {
        return programme_status_resource(id, root);
    }
    if uri == "icstudio://project" || uri.starts_with("icstudio://project/revision/") {
        return project_resource(id, &uri, active_project);
    }
    error_response(id, -32002, &format!("unknown resource: {uri}"))
}

fn programme_status_resource(id: &str, root: &Path) -> String {
    let capabilities = match read_required(&root.join(CAPABILITIES_PATH)) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    let truth = match read_required(&root.join(TRUTH_PATH)) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    let body = format!(
        "{{\"capabilities\":{},\"truth\":{}}}",
        capabilities.trim(),
        truth.trim()
    );
    text_resource(id, "icstudio://status", &body)
}

fn project_resource(id: &str, uri: &str, active_project: Option<&Path>) -> String {
    let path = match active_project {
        Some(value) => value,
        None => {
            return error_response(id, -32003, "ICSTUDIO_ACTIVE_PROJECT is not configured");
        }
    };
    let store = match ProjectStore::open(path) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    if let Some(requested) = uri.strip_prefix("icstudio://project/revision/") {
        let requested = match requested.parse::<u64>() {
            Ok(value) => value,
            Err(error) => {
                return error_response(
                    id,
                    -32602,
                    &format!("invalid project revision '{requested}': {error}"),
                );
            }
        };
        if requested != store.project().revision {
            return error_response(
                id,
                -32004,
                &format!(
                    "project revision conflict: requested {requested}, current {}",
                    store.project().revision
                ),
            );
        }
    }
    let canonical_uri = format!("icstudio://project/revision/{}", store.project().revision);
    text_resource(id, &canonical_uri, &store.project().summary_json())
}

fn text_resource(id: &str, uri: &str, body: &str) -> String {
    success(
        id,
        &format!(
            "{{\"contents\":[{{\"uri\":\"{}\",\"mimeType\":\"application/json\",\"text\":\"{}\"}}]}}",
            escape_json(uri),
            escape_json(body)
        ),
    )
}

fn tools_list(id: &str) -> String {
    success(
        id,
        "{\"tools\":[{\"name\":\"capability.report\",\"title\":\"Report ICStudio capabilities\",\"description\":\"Return the evidence-backed capability matrix and conservative truth score.\",\"inputSchema\":{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false},\"annotations\":{\"readOnlyHint\":true,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":false}},{\"name\":\"project.inspect\",\"title\":\"Inspect active ICStudio project\",\"description\":\"Return the same authoritative project and revision summary exposed by the CLI.\",\"inputSchema\":{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false},\"annotations\":{\"readOnlyHint\":true,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":false}}]}",
    )
}

fn tools_call(id: &str, params: &Object, root: &Path, active_project: Option<&Path>) -> String {
    let name = match params.required_string("name") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    match name.as_str() {
        "capability.report" => capability_tool(id, root),
        "project.inspect" => project_inspect_tool(id, active_project),
        _ => error_response(id, -32602, &format!("unknown tool: {name}")),
    }
}

fn capability_tool(id: &str, root: &Path) -> String {
    let report = match capability_report_markdown(root) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    let score = match truth_score(root) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    success(
        id,
        &format!(
            "{{\"content\":[{{\"type\":\"text\",\"text\":\"{}\"}}],\"structuredContent\":{{\"truthScore\":{score:.2},\"maximumScore\":100,\"milestone\":\"M1\",\"claim\":\"project database foundation in development\"}},\"isError\":false}}",
            escape_json(&report)
        ),
    )
}

fn project_inspect_tool(id: &str, active_project: Option<&Path>) -> String {
    let path = match active_project {
        Some(value) => value,
        None => {
            return error_response(id, -32003, "ICSTUDIO_ACTIVE_PROJECT is not configured");
        }
    };
    let store = match ProjectStore::open(path) {
        Ok(value) => value,
        Err(error) => return error_response(id, -32001, &error),
    };
    let summary = store.project().summary_json();
    success(
        id,
        &format!(
            "{{\"content\":[{{\"type\":\"text\",\"text\":\"{}\"}}],\"structuredContent\":{},\"isError\":false}}",
            escape_json(&summary),
            summary
        ),
    )
}

fn prompts_list(id: &str) -> String {
    success(
        id,
        "{\"prompts\":[{\"name\":\"icstudio.m0.status\",\"title\":\"Review ICStudio programme status\",\"description\":\"Evaluate capability evidence without overstating implementation.\",\"arguments\":[]},{\"name\":\"icstudio.project.review\",\"title\":\"Review active project hierarchy\",\"description\":\"Inspect the bounded revision-addressed project summary.\",\"arguments\":[]}]}",
    )
}

fn prompts_get(id: &str, params: &Object) -> String {
    let name = match params.required_string("name") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    match name.as_str() {
        "icstudio.m0.status" => success(
            id,
            "{\"description\":\"Evidence-first milestone review\",\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"Read icstudio://status, call capability.report, identify unverified claims, and report the truth score. Do not infer solver or editor functionality that is not supported by evidence.\"}}]}",
        ),
        "icstudio.project.review" => success(
            id,
            "{\"description\":\"Revision-safe project review\",\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"Call project.inspect and report the active project ID, revision, and hierarchy counts. Treat the returned revision as immutable context and do not request mutations.\"}}]}",
        ),
        _ => error_response(id, -32602, &format!("unknown prompt: {name}")),
    }
}

fn success(id: &str, result: &str) -> String {
    format!("{{\"jsonrpc\":\"2.0\",\"id\":{id},\"result\":{result}}}")
}

fn error_response(id: &str, code: i32, message: &str) -> String {
    format!(
        "{{\"jsonrpc\":\"2.0\",\"id\":{id},\"error\":{{\"code\":{code},\"message\":\"{}\"}}}}",
        escape_json(message)
    )
}
