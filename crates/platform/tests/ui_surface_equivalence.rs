use icstudio_project::{ProjectStore, Transaction};
use std::io::{BufRead, BufReader, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{SystemTime, UNIX_EPOCH};

fn temporary_project() -> PathBuf {
    let unique = format!(
        "icstudio-ui-equivalence-{}-{}",
        std::process::id(),
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("time")
            .as_nanos()
    );
    std::env::temp_dir().join(unique)
}

fn stdout(command: &mut Command) -> String {
    let output = command.output().expect("run surface");
    assert!(
        output.status.success(),
        "surface failed: {}",
        String::from_utf8_lossy(&output.stderr)
    );
    String::from_utf8(output.stdout)
        .expect("UTF-8 surface output")
        .trim()
        .to_string()
}

fn repository_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../..")
        .canonicalize()
        .expect("repository root")
}

#[test]
fn cli_ui_and_mcp_display_identical_project_state_and_revision() {
    let root = temporary_project();
    let mut store = ProjectStore::create(&root, "equivalence").expect("create project");
    store
        .commit(Transaction::new(0, "test-library", "integration").add_library("analog"))
        .expect("add library");
    store
        .commit(Transaction::new(1, "test-cell", "integration").add_cell("analog", "inverter"))
        .expect("add cell");
    store
        .commit(Transaction::new(2, "test-view", "integration").add_view(
            "analog",
            "inverter",
            "schematic",
            "schematic",
        ))
        .expect("add view");
    let expected = store.project().summary_json();

    let cli = stdout(
        Command::new(env!("CARGO_BIN_EXE_icstudio"))
            .arg("project")
            .arg("show")
            .arg("--path")
            .arg(&root),
    );
    let ui = stdout(
        Command::new(env!("CARGO_BIN_EXE_icstudio-ui-bridge"))
            .arg("--path")
            .arg(&root),
    );

    assert_eq!(cli, expected);
    assert_eq!(ui, expected);

    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-mcp"))
        .env("ICSTUDIO_PROJECT_ROOT", repository_root())
        .env("ICSTUDIO_ACTIVE_PROJECT", &root)
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("spawn MCP server");
    let mut stdin = child.stdin.take().expect("MCP stdin");
    writeln!(
        stdin,
        "{{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{{\"name\":\"project.inspect\",\"arguments\":{{}}}}}}"
    )
    .expect("write MCP request");
    drop(stdin);

    let mut response = String::new();
    BufReader::new(child.stdout.take().expect("MCP stdout"))
        .read_line(&mut response)
        .expect("read MCP response");
    let status = child.wait().expect("wait for MCP server");
    assert!(status.success());
    assert!(
        response.contains(&format!("\"structuredContent\":{expected}")),
        "MCP response did not contain the canonical snapshot: {response}"
    );

    std::fs::remove_dir_all(root).expect("cleanup");
}
