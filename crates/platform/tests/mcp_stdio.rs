use icstudio_platform::escape_json;
use icstudio_project::{ProjectStore, Transaction};
use std::fs;
use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{SystemTime, UNIX_EPOCH};

fn repository_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .canonicalize()
        .expect("repository root")
}

fn temporary_root(label: &str) -> PathBuf {
    let unique = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("clock")
        .as_nanos();
    std::env::temp_dir().join(format!(
        "icstudio-mcp-{label}-{}-{unique}",
        std::process::id()
    ))
}

#[test]
fn stdio_server_negotiates_and_lists_the_read_only_surface() {
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
        "{{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{{}},\"clientInfo\":{{\"name\":\"m1-test\",\"version\":\"0.1\"}}}}}}"
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
    assert!(tools.contains("project.inspect"));
    assert!(prompts.contains("icstudio.project.review"));
    assert!(report.contains("\"truthScore\":5.00"));
    assert!(report.contains("project database foundation in development"));

    let status = child.wait().expect("wait for MCP server");
    assert!(status.success());
}

#[test]
fn stdio_server_uses_top_level_correlation_fields() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-mcp"))
        .env("ICSTUDIO_PROJECT_ROOT", repository_root())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("spawn MCP server");
    let mut stdin = child.stdin.take().expect("child stdin");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"params\":{{\"id\":999,\"method\":\"tools/list\"}},\"id\":77,\"method\":\"ping\"}}"
    )
    .expect("nested-field request");
    drop(stdin);

    let mut response = String::new();
    BufReader::new(child.stdout.take().expect("child stdout"))
        .read_line(&mut response)
        .expect("response");
    assert!(response.contains("\"id\":77"));
    assert!(!response.contains("\"id\":999"));
    assert!(response.contains("\"result\":{}"));
    assert!(child.wait().expect("wait").success());
}

#[test]
fn cli_and_mcp_expose_the_same_project_state_and_revision() {
    let project_root = temporary_root("project-state");
    let mut store = ProjectStore::create(&project_root, "shared").expect("create project");
    store
        .commit(
            Transaction::new(0, "shared-state", "test")
                .add_library("analog")
                .add_cell("analog", "inverter")
                .add_view("analog", "inverter", "schematic", "schematic"),
        )
        .expect("commit project");
    let summary = store.project().summary_json();
    let revision = store.project().revision;
    drop(store);

    let cli = Command::new(env!("CARGO_BIN_EXE_icstudio"))
        .arg("--project-root")
        .arg(repository_root())
        .args(["project", "show", "--path"])
        .arg(&project_root)
        .output()
        .expect("run CLI");
    assert!(cli.status.success());
    assert_eq!(String::from_utf8_lossy(&cli.stdout).trim(), summary);

    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-mcp"))
        .env("ICSTUDIO_PROJECT_ROOT", repository_root())
        .env("ICSTUDIO_ACTIVE_PROJECT", &project_root)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("spawn MCP server");
    let mut stdin = child.stdin.take().expect("child stdin");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{{\"protocolVersion\":\"2025-11-25\"}}}}"
    )
    .expect("initialize");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"resources/list\",\"params\":{{}}}}"
    )
    .expect("resource list");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"resources/read\",\"params\":{{\"uri\":\"icstudio://project/revision/{revision}\"}}}}"
    )
    .expect("resource read");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{{\"name\":\"project.inspect\",\"arguments\":{{}}}}}}"
    )
    .expect("project inspect");
    drop(stdin);

    let stdout = child.stdout.take().expect("child stdout");
    let mut lines = BufReader::new(stdout).lines();
    let initialize = lines.next().expect("initialize response").expect("line");
    let resources = lines.next().expect("resources response").expect("line");
    let read = lines.next().expect("read response").expect("line");
    let inspect = lines.next().expect("inspect response").expect("line");
    assert!(initialize.contains("M1 read-only project gateway"));
    assert!(resources.contains(&format!("icstudio://project/revision/{revision}")));
    assert!(read.contains(&escape_json(&summary)));
    assert!(inspect.contains(&escape_json(&summary)));
    assert!(inspect.contains(&summary));
    assert!(child.wait().expect("wait").success());

    fs::remove_dir_all(project_root).expect("cleanup");
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
        "{{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"initialize\",\"params\":{{\"protocolVersion\":\"1900-01-01\",\"capabilities\":{{}},\"clientInfo\":{{\"name\":\"m1-test\",\"version\":\"0.1\"}}}}}}"
    )
    .expect("request");
    drop(stdin);
    let mut response = String::new();
    BufReader::new(child.stdout.take().expect("child stdout"))
        .read_line(&mut response)
        .expect("response");
    assert!(response.contains("unsupported MCP protocol revision"));
    assert!(response.contains("2025-11-25"));
    assert!(response.contains("\"code\":-32602"));
    assert!(child.wait().expect("wait").success());
}
