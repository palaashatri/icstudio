use icstudio_platform::{
    capability_report_markdown, escape_json, extract_named_string, project_root_from_env,
    read_required, truth_score, CAPABILITIES_PATH, MCP_PROTOCOL_VERSION, TRUTH_PATH,
};
use std::io::{self, BufRead, Write};

fn main() {
    let root = project_root_from_env();
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

        let method = match extract_named_string(&line, "method") {
            Ok(value) => value,
            Err(error) => {
                let response = error_response("null", -32600, &error);
                emit(&mut stdout, &response);
                continue;
            }
        };
        if method.starts_with("notifications/") {
            continue;
        }
        let id = extract_raw_id(&line).unwrap_or_else(|| "null".to_string());
        let response = match method.as_str() {
            "initialize" => initialize_response(&id, &line),
            "ping" => success(&id, "{}"),
            "resources/list" => resources_list(&id),
            "resources/read" => resources_read(&id, &line, &root),
            "tools/list" => tools_list(&id),
            "tools/call" => tools_call(&id, &line, &root),
            "prompts/list" => prompts_list(&id),
            "prompts/get" => prompts_get(&id, &line),
            _ => error_response(&id, -32601, &format!("method not found: {method}")),
        };
        emit(&mut stdout, &response);
    }
}

fn emit(stdout: &mut impl Write, response: &str) {
    if writeln!(stdout, "{response}").is_err() || stdout.flush().is_err() {
        eprintln!("icstudio-mcp: failed to write stdout");
    }
}

fn initialize_response(id: &str, request: &str) -> String {
    let requested = extract_named_string(request, "protocolVersion")
        .unwrap_or_else(|_| MCP_PROTOCOL_VERSION.to_string());
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
            "{{\"protocolVersion\":\"{MCP_PROTOCOL_VERSION}\",\"capabilities\":{{\"resources\":{{\"subscribe\":false,\"listChanged\":false}},\"tools\":{{\"listChanged\":false}},\"prompts\":{{\"listChanged\":false}},\"logging\":{{}}}},\"serverInfo\":{{\"name\":\"icstudio-mcp\",\"version\":\"0.1.0\"}},\"instructions\":\"M0 read-only factory gateway. No circuit mutation or solver capability is claimed.\"}}"
        ),
    )
}

fn resources_list(id: &str) -> String {
    success(
        id,
        "{\"resources\":[{\"uri\":\"icstudio://status\",\"name\":\"ICStudio programme status\",\"description\":\"Revision-independent M0 capability and truth state\",\"mimeType\":\"application/json\"}]}"
    )
}

fn resources_read(id: &str, request: &str, root: &std::path::Path) -> String {
    let uri = match extract_named_string(request, "uri") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    if uri != "icstudio://status" {
        return error_response(id, -32002, &format!("unknown resource: {uri}"));
    }
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
    success(
        id,
        &format!(
            "{{\"contents\":[{{\"uri\":\"icstudio://status\",\"mimeType\":\"application/json\",\"text\":\"{}\"}}]}}",
            escape_json(&body)
        ),
    )
}

fn tools_list(id: &str) -> String {
    success(
        id,
        "{\"tools\":[{\"name\":\"capability.report\",\"title\":\"Report ICStudio capabilities\",\"description\":\"Return the evidence-backed capability matrix and conservative truth score.\",\"inputSchema\":{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false},\"annotations\":{\"readOnlyHint\":true,\"destructiveHint\":false,\"idempotentHint\":true,\"openWorldHint\":false}}]}"
    )
}

fn tools_call(id: &str, request: &str, root: &std::path::Path) -> String {
    let name = match extract_named_string(request, "name") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    if name != "capability.report" {
        return error_response(id, -32602, &format!("unknown tool: {name}"));
    }
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
            "{{\"content\":[{{\"type\":\"text\",\"text\":\"{}\"}}],\"structuredContent\":{{\"truthScore\":{score:.2},\"maximumScore\":100,\"milestone\":\"M0\",\"claim\":\"factory scaffold only\"}},\"isError\":false}}",
            escape_json(&report)
        ),
    )
}

fn prompts_list(id: &str) -> String {
    success(
        id,
        "{\"prompts\":[{\"name\":\"icstudio.m0.status\",\"title\":\"Review M0 factory status\",\"description\":\"Ask an LLM to evaluate the current capability evidence without overstating implementation.\",\"arguments\":[]}]}"
    )
}

fn prompts_get(id: &str, request: &str) -> String {
    let name = match extract_named_string(request, "name") {
        Ok(value) => value,
        Err(error) => return error_response(id, -32602, &error),
    };
    if name != "icstudio.m0.status" {
        return error_response(id, -32602, &format!("unknown prompt: {name}"));
    }
    success(
        id,
        "{\"description\":\"Evidence-first milestone review\",\"messages\":[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"Read icstudio://status, call capability.report, identify unverified claims, and report the truth score. Do not infer solver or editor functionality that is not supported by evidence.\"}}]}"
    )
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

fn extract_raw_id(input: &str) -> Option<String> {
    let key = input.find("\"id\"")?;
    let tail = &input[key + "\"id\"".len()..];
    let colon = tail.find(':')?;
    let value = tail[colon + 1..].trim_start();
    if let Some(stripped) = value.strip_prefix('"') {
        let mut escaped = false;
        for (index, character) in stripped.char_indices() {
            if escaped {
                escaped = false;
            } else if character == '\\' {
                escaped = true;
            } else if character == '"' {
                return Some(value[..index + 2].to_string());
            }
        }
        None
    } else {
        let length = value
            .find(|character: char| {
                character == ',' || character == '}' || character.is_whitespace()
            })
            .unwrap_or(value.len());
        Some(value[..length].to_string())
    }
}
