use std::process::Command;

#[test]
fn project_root_without_value_reports_the_missing_value() {
    let output = Command::new(env!("CARGO_BIN_EXE_icstudio"))
        .arg("--project-root")
        .output()
        .expect("run CLI");
    assert!(!output.status.success());
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(stderr.contains("--project-root requires a value"));
    assert!(!stderr.contains("unknown command '--project-root'"));
}

#[test]
fn output_without_value_reports_the_missing_value() {
    let output = Command::new(env!("CARGO_BIN_EXE_icstudio"))
        .args(["capabilities", "--output"])
        .output()
        .expect("run CLI");
    assert!(!output.status.success());
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(stderr.contains("--output requires a value"));
    assert!(!stderr.contains("unexpected arguments: --output"));
}

#[test]
fn an_option_cannot_consume_the_next_option_as_its_value() {
    let output = Command::new(env!("CARGO_BIN_EXE_icstudio"))
        .args(["capabilities", "--output", "--project-root", "."])
        .output()
        .expect("run CLI");
    assert!(!output.status.success());
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(
        stderr.contains("--project-root requires a value")
            || stderr.contains("--output requires a value")
    );
}
