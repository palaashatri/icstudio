use std::fs;
use std::path::PathBuf;

fn repository_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .parent()
        .and_then(|path| path.parent())
        .expect("platform crate must live under crates/platform")
        .to_path_buf()
}

#[test]
fn authoritative_constitution_requires_java_25_swing_stack() {
    assert!(
        repository_root().join("AGENTS.md").is_file(),
        "AGENTS.md agent guide must exist at the repository root"
    );
    let agents = fs::read_to_string(repository_root().join("docs/CONSTITUTION.md"))
        .expect("docs/CONSTITUTION.md must be readable");

    assert!(
        agents.contains("Java 25"),
        "the constitution must name Java 25 as the authoritative application language"
    );
    assert!(
        agents.contains("Swing"),
        "the constitution must name Swing as the desktop toolkit"
    );
    assert!(
        agents.contains("Skija") || agents.contains("Skia"),
        "the constitution must define Skia-backed engineering canvases"
    );
    assert!(
        agents.contains("Foreign Function and Memory API") || agents.contains("FFM"),
        "the constitution must define Java FFM as the preferred native boundary"
    );
    assert!(
        !agents.contains("Electron + React/TypeScript shell"),
        "the old Electron/React authoritative architecture must be removed"
    );
    assert!(
        !agents.contains("Rust Platform Services"),
        "Rust must not remain the authoritative application platform after ADR-0001"
    );
}
