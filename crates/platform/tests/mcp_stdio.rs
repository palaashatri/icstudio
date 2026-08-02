use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};

fn repository_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .canonicalize()
        .expect("repository root")
}

#[test]
fn stdio_server_negotiates_and_lists_the_m0_surface() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-mcp"))
        .env("ICSTUDIO_PROJECT_ROOT", repository_root())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .expect("spawn MCP server");

    let mut stdin = child.stdin.take().expect("child stdin");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{{}},\"clientInfo\":{{\"name\":\"m0-test\",\"version\":\"0.1\"}}}}}}"
    )
    .expect("initialize request");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{{}}}}"
    )
    .expect("initialized notification");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"resources/list\",\"params\":{{}}}}"
    )
    .expect("resources request");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{{}}}}"
    )
    .expect("tools request");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"prompts/list\",\"params\":{{}}}}"
    )
    .expect("prompts request");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{{\"name\":\"capability.report\",\"arguments\":{{}}}}}}"
    )
    .expect("tool request");
    drop(stdin);

    let stdout = child.stdout.take().expect("child stdout");
    let mut lines = BufReader::new(stdout).lines();
    let initialize = lines.next().expect("initialize response").expect("line");
    let resources = lines.next().expect("resources response").expect("line");
    let tools = lines.next().expect("tools response").expect("line");
    let prompts = lines.next().expect("prompts response").expect("line");
    let report = lines.next().expect("tool response").expect("line");

    assert!(initialize.contains("\"protocolVersion\":\"2025-11-25\""));
    assert!(initialize.contains("\"name\":\"icstudio-mcp\""));
    assert!(resources.contains("icstudio://status"));
    assert!(tools.contains("capability.report"));
    assert!(prompts.contains("icstudio.m0.status"));
    assert!(report.contains("\"truthScore\":2.00"));
    assert!(report.contains("factory scaffold only"));

    let status = child.wait().expect("wait for MCP server");
    assert!(status.success());
}

#[test]
fn stdio_server_rejects_an_unlocked_protocol_revision() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-mcp"))
        .env("ICSTUDIO_PROJECT_ROOT", repository_root())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("spawn MCP server");
    let mut stdin = child.stdin.take().expect("child stdin");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"initialize\",\"params\":{{\"protocolVersion\":\"1900-01-01\"}}}}"
    )
    .expect("request");
    drop(stdin);
    let mut response = String::new();
    BufReader::new(child.stdout.take().expect("child stdout"))
        .read_line(&mut response)
        .expect("response");
    assert!(response.contains("unsupported MCP protocol revision"));
    assert!(response.contains("\"code\":-32602"));
    assert!(child.wait().expect("wait").success());
}
