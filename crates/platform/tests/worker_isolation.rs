use icstudio_project::ProjectStore;
use icstudio_rpc::{RequestEnvelope, ResponseEnvelope};
use std::fs;
use std::io::{BufRead, BufReader, Write};
use std::path::PathBuf;
use std::process::{Command, Stdio};
use std::time::{SystemTime, UNIX_EPOCH};

fn temporary_root(label: &str) -> PathBuf {
    let unique = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .expect("clock")
        .as_nanos();
    std::env::temp_dir().join(format!(
        "icstudio-worker-{label}-{}-{unique}",
        std::process::id()
    ))
}

#[test]
fn worker_uses_the_versioned_request_and_response_envelope() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-worker"))
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .spawn()
        .expect("spawn worker");
    let request = RequestEnvelope {
        request_id: "ping-1".to_string(),
        project_id: "0123456789abcdef0123456789abcdef".to_string(),
        expected_revision: 7,
        command: "worker.ping".to_string(),
        payload: "health-check".to_string(),
    };
    let mut stdin = child.stdin.take().expect("worker stdin");
    writeln!(stdin, "{}", request.encode().expect("encode request")).expect("write request");
    drop(stdin);

    let mut line = String::new();
    BufReader::new(child.stdout.take().expect("worker stdout"))
        .read_line(&mut line)
        .expect("read response");
    let response = ResponseEnvelope::decode(&line).expect("decode response");
    assert!(response.success);
    assert_eq!(response.request_id, "ping-1");
    assert!(response.payload.contains("revision=7"));
    assert!(child.wait().expect("wait for worker").success());
}

#[test]
fn worker_crash_does_not_corrupt_or_terminate_the_platform() {
    let project_root = temporary_root("crash");
    let store = ProjectStore::create(&project_root, "survivor").expect("create project");
    let summary = store.project().summary_json();
    let project_id = store.project().id.to_string();
    drop(store);

    let mut child = Command::new(env!("CARGO_BIN_EXE_icstudio-worker"))
        .stdin(Stdio::piped())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .expect("spawn worker");
    let request = RequestEnvelope {
        request_id: "crash-1".to_string(),
        project_id,
        expected_revision: 0,
        command: "worker.crash".to_string(),
        payload: String::new(),
    };
    let mut stdin = child.stdin.take().expect("worker stdin");
    writeln!(stdin, "{}", request.encode().expect("encode request")).expect("write request");
    drop(stdin);
    assert!(!child.wait().expect("wait for crashing worker").success());

    let reopened = ProjectStore::open(&project_root).expect("reopen after worker crash");
    assert_eq!(reopened.project().summary_json(), summary);
    let cli = Command::new(env!("CARGO_BIN_EXE_icstudio"))
        .args(["project", "show", "--path"])
        .arg(&project_root)
        .output()
        .expect("run platform CLI after worker crash");
    assert!(cli.status.success());
    assert_eq!(String::from_utf8_lossy(&cli.stdout).trim(), summary);

    fs::remove_dir_all(project_root).expect("cleanup");
}
