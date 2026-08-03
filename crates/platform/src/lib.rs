//! ICStudio platform primitives and programme-evidence services.
//!
//! The implementation remains dependency-free while M1 contracts are being frozen.
//! Parsing and evidence operations are deliberately small, deterministic, and covered by
//! regression tests before they are replaced by richer schema-generated implementations.

use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::time::{SystemTime, UNIX_EPOCH};

pub const MCP_PROTOCOL_VERSION: &str = "2025-11-25";
pub const CAPABILITIES_PATH: &str = ".project/capabilities.json";
pub const TRUTH_PATH: &str = ".project/truth.json";

const REQUIRED_CAPABILITIES: &[&str] = &[
    "CAP-INFRA-BOOT",
    "CAP-PROJ-DB",
    "CAP-MCP-BASE",
    "CAP-SIM-DC",
    "CAP-LAY-EDIT",
    "CAP-DRC",
    "CAP-LVS",
    "CAP-PEX-RC",
    "CAP-EM-3D",
    "CAP-PHOTONICS",
    "CAP-FLOW-OPENPDK",
    "CAP-RELEASE",
];

const WORKSPACE_CRATES: &[(&str, &str)] = &[
    ("icstudio-geometry", "crates/geometry/Cargo.toml"),
    ("icstudio-platform", "crates/platform/Cargo.toml"),
    ("icstudio-project", "crates/project/Cargo.toml"),
    ("icstudio-rpc", "crates/rpc/Cargo.toml"),
];

pub fn project_root_from_env() -> PathBuf {
    std::env::var_os("ICSTUDIO_PROJECT_ROOT")
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("."))
}

pub fn read_required(path: &Path) -> Result<String, String> {
    fs::read_to_string(path).map_err(|error| format!("failed to read {}: {error}", path.display()))
}

pub fn truth_score(root: &Path) -> Result<f64, String> {
    let input = read_required(&root.join(TRUTH_PATH))?;
    let mut cursor = 0;
    let mut score = 0.0;
    let mut milestones = 0;

    while let Some(id_rel) = input[cursor..].find("\"id\"") {
        let id_key = cursor + id_rel;
        let next_id = input[id_key + 1..]
            .find("\"id\"")
            .map(|value| id_key + 1 + value)
            .unwrap_or(input.len());
        let object = &input[id_key..next_id];
        let weight = object
            .find("\"weight\"")
            .ok_or_else(|| "truth milestone is missing weight".to_string())
            .and_then(|index| parse_number_after(object, index + "\"weight\"".len()))?;
        let completion = object
            .find("\"completion\"")
            .ok_or_else(|| "truth milestone is missing completion".to_string())
            .and_then(|index| parse_number_after(object, index + "\"completion\"".len()))?;
        if !(0.0..=1.0).contains(&completion) {
            return Err(format!(
                "milestone completion must be between 0 and 1, got {completion}"
            ));
        }
        score += weight * completion;
        milestones += 1;
        cursor = next_id;
    }

    if milestones == 0 {
        return Err("truth file contains no milestone records".to_string());
    }
    Ok(score)
}

pub fn reported_truth_score(root: &Path) -> Result<f64, String> {
    let input = read_required(&root.join(TRUTH_PATH))?;
    let key = input
        .find("\"reported_score\"")
        .ok_or_else(|| "truth file is missing reported_score".to_string())?;
    parse_number_after(&input, key + "\"reported_score\"".len())
}

pub fn validate_project_state(root: &Path) -> Result<(), String> {
    for relative in [
        "AGENTS.md",
        "LICENSE",
        "Cargo.toml",
        "Cargo.lock",
        CAPABILITIES_PATH,
        TRUTH_PATH,
        "toolchains/mcp.lock",
    ] {
        let path = root.join(relative);
        if !path.is_file() {
            return Err(format!(
                "required project file is missing: {}",
                path.display()
            ));
        }
    }

    let capabilities = read_required(&root.join(CAPABILITIES_PATH))?;
    for capability in REQUIRED_CAPABILITIES {
        if !capabilities.contains(capability) {
            return Err(format!("capability state is missing {capability}"));
        }
    }

    let protocol_lock = read_required(&root.join("toolchains/mcp.lock"))?;
    if protocol_lock.trim() != MCP_PROTOCOL_VERSION {
        return Err(format!(
            "MCP lock mismatch: expected {MCP_PROTOCOL_VERSION}, found {}",
            protocol_lock.trim()
        ));
    }

    let computed = truth_score(root)?;
    let reported = reported_truth_score(root)?;
    if (computed - reported).abs() > 1e-9 {
        return Err(format!(
            "truth score mismatch: computed {computed:.2}, reported {reported:.2}"
        ));
    }

    license_check(root)?;
    Ok(())
}

pub fn capability_report_markdown(root: &Path) -> Result<String, String> {
    let input = read_required(&root.join(CAPABILITIES_PATH))?;
    let mut cursor = 0;
    let mut rows = Vec::new();

    while let Some(id_rel) = input[cursor..].find("\"id\"") {
        let id_key = cursor + id_rel;
        let id = parse_string_after(&input, id_key + "\"id\"".len())?;
        let next_id = input[id_key + 1..]
            .find("\"id\"")
            .map(|value| id_key + 1 + value)
            .unwrap_or(input.len());
        let object = &input[id_key..next_id];
        let name = object
            .find("\"name\"")
            .and_then(|index| parse_string_after(object, index + "\"name\"".len()).ok())
            .unwrap_or_else(|| id.clone());
        let status = object
            .find("\"status\"")
            .and_then(|index| parse_string_after(object, index + "\"status\"".len()).ok())
            .unwrap_or_else(|| "unknown".to_string());
        let tier = object
            .find("\"tier\"")
            .and_then(|index| parse_number_after(object, index + "\"tier\"".len()).ok())
            .unwrap_or(0.0);
        rows.push((id, name, status, tier as u8));
        cursor = next_id;
    }

    if rows.is_empty() {
        return Err("capability state contains no capability records".to_string());
    }

    let score = truth_score(root)?;
    let mut report = String::from("# ICStudio capability report\n\n");
    report.push_str(&format!("**Truth score:** {score:.2}/100\n\n"));
    report.push_str("| Capability | Description | Status | Tier |\n");
    report.push_str("|---|---|---:|---:|\n");
    for (id, name, status, tier) in rows {
        report.push_str(&format!("| `{id}` | {name} | {status} | {tier} |\n"));
    }
    Ok(report)
}

pub fn create_checkpoint(root: &Path, name: &str) -> Result<PathBuf, String> {
    validate_checkpoint_name(name)?;
    validate_project_state(root)?;

    let checkpoint_dir = root.join(".project/checkpoints");
    fs::create_dir_all(&checkpoint_dir)
        .map_err(|error| format!("failed to create {}: {error}", checkpoint_dir.display()))?;
    let destination = checkpoint_dir.join(format!("{name}.json"));

    let capabilities = read_required(&root.join(CAPABILITIES_PATH))?;
    let truth = read_required(&root.join(TRUTH_PATH))?;
    let created_unix = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|error| format!("system clock is before UNIX epoch: {error}"))?
        .as_secs();
    let git_commit = current_git_commit(root).unwrap_or_else(|| "unknown".to_string());
    let manifest = format!(
        "{{\n  \"schema_version\": 1,\n  \"name\": \"{}\",\n  \"created_unix\": {},\n  \"git_commit\": \"{}\",\n  \"capabilities_hash\": \"{}\",\n  \"truth_hash\": \"{}\",\n  \"truth_score\": {:.2}\n}}\n",
        escape_json(name),
        created_unix,
        escape_json(&git_commit),
        fnv1a64(capabilities.as_bytes()),
        fnv1a64(truth.as_bytes()),
        truth_score(root)?
    );
    let mut file = fs::OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&destination)
        .map_err(|error| {
            if error.kind() == std::io::ErrorKind::AlreadyExists {
                format!("checkpoint already exists: {}", destination.display())
            } else {
                format!("failed to create {}: {error}", destination.display())
            }
        })?;
    file.write_all(manifest.as_bytes())
        .map_err(|error| format!("failed to write {}: {error}", destination.display()))?;
    file.sync_all()
        .map_err(|error| format!("failed to sync {}: {error}", destination.display()))?;
    Ok(destination)
}

pub fn resume_check(root: &Path, checkpoint_name: &str) -> Result<(), String> {
    validate_checkpoint_name(checkpoint_name)?;
    validate_project_state(root)?;
    let path = root
        .join(".project/checkpoints")
        .join(format!("{checkpoint_name}.json"));
    let checkpoint = read_required(&path)?;
    let expected_capabilities = extract_named_string(&checkpoint, "capabilities_hash")?;
    let expected_truth = extract_named_string(&checkpoint, "truth_hash")?;
    let capabilities = read_required(&root.join(CAPABILITIES_PATH))?;
    let truth = read_required(&root.join(TRUTH_PATH))?;
    let actual_capabilities = fnv1a64(capabilities.as_bytes());
    let actual_truth = fnv1a64(truth.as_bytes());

    if expected_capabilities != actual_capabilities {
        return Err(format!(
            "capability state differs from checkpoint {checkpoint_name}: expected {expected_capabilities}, got {actual_capabilities}"
        ));
    }
    if expected_truth != actual_truth {
        return Err(format!(
            "truth state differs from checkpoint {checkpoint_name}: expected {expected_truth}, got {actual_truth}"
        ));
    }
    Ok(())
}

pub fn license_check(root: &Path) -> Result<(), String> {
    let workspace = read_required(&root.join("Cargo.toml"))?;
    let lock = read_required(&root.join("Cargo.lock"))?;

    if !workspace.contains("license = \"MIT\"") {
        return Err("workspace package licence is not MIT".to_string());
    }
    for (crate_name, manifest_path) in WORKSPACE_CRATES {
        let manifest = read_required(&root.join(manifest_path))?;
        if !manifest.contains("license.workspace = true") {
            return Err(format!(
                "{crate_name} does not inherit the workspace MIT licence"
            ));
        }
    }
    if lock.contains("source =") {
        return Err(
            "dependency policy violation: Cargo.lock contains an external package source"
                .to_string(),
        );
    }
    Ok(())
}

pub fn write_sbom(root: &Path, destination: &Path) -> Result<(), String> {
    validate_project_state(root)?;
    let document_namespace = format!(
        "https://github.com/palaashatri/icstudio/sbom/{}",
        fnv1a64(read_required(&root.join("Cargo.lock"))?.as_bytes())
    );
    let packages = WORKSPACE_CRATES
        .iter()
        .map(|(name, _)| {
            format!(
                "    {{\n      \"name\": \"{name}\",\n      \"SPDXID\": \"SPDXRef-Package-{name}\",\n      \"versionInfo\": \"0.1.0\",\n      \"downloadLocation\": \"NOASSERTION\",\n      \"filesAnalyzed\": false,\n      \"licenseConcluded\": \"MIT\",\n      \"licenseDeclared\": \"MIT\",\n      \"copyrightText\": \"Copyright (c) 2026 ICStudio contributors\"\n    }}"
            )
        })
        .collect::<Vec<_>>()
        .join(",\n");
    let sbom = format!(
        "{{\n  \"spdxVersion\": \"SPDX-2.3\",\n  \"dataLicense\": \"CC0-1.0\",\n  \"SPDXID\": \"SPDXRef-DOCUMENT\",\n  \"name\": \"icstudio-implementation\",\n  \"documentNamespace\": \"{document_namespace}\",\n  \"creationInfo\": {{\n    \"creators\": [\"Tool: icstudio-sbom-bootstrap\"]\n  }},\n  \"packages\": [\n{packages}\n  ]\n}}\n"
    );
    write_text(destination, &sbom)
}

pub fn write_text(path: &Path, contents: &str) -> Result<(), String> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)
            .map_err(|error| format!("failed to create {}: {error}", parent.display()))?;
    }
    let mut file = fs::File::create(path)
        .map_err(|error| format!("failed to create {}: {error}", path.display()))?;
    file.write_all(contents.as_bytes())
        .map_err(|error| format!("failed to write {}: {error}", path.display()))
}

pub fn fnv1a64(bytes: &[u8]) -> String {
    let mut hash = 0xcbf29ce484222325_u64;
    for byte in bytes {
        hash ^= u64::from(*byte);
        hash = hash.wrapping_mul(0x100000001b3);
    }
    format!("{hash:016x}")
}

pub fn escape_json(value: &str) -> String {
    let mut escaped = String::with_capacity(value.len());
    for character in value.chars() {
        match character {
            '"' => escaped.push_str("\\\""),
            '\\' => escaped.push_str("\\\\"),
            '\n' => escaped.push_str("\\n"),
            '\r' => escaped.push_str("\\r"),
            '\t' => escaped.push_str("\\t"),
            character if character.is_control() => {
                escaped.push_str(&format!("\\u{:04x}", character as u32));
            }
            character => escaped.push(character),
        }
    }
    escaped
}

pub fn extract_named_string(input: &str, key: &str) -> Result<String, String> {
    let quoted_key = format!("\"{key}\"");
    let position = input
        .find(&quoted_key)
        .ok_or_else(|| format!("missing JSON key {key}"))?;
    parse_string_after(input, position + quoted_key.len())
}

fn parse_number_after(input: &str, after: usize) -> Result<f64, String> {
    let tail = input
        .get(after..)
        .ok_or_else(|| "invalid number offset".to_string())?;
    let colon = tail
        .find(':')
        .ok_or_else(|| "JSON number is missing ':'".to_string())?;
    let numeric = tail[colon + 1..].trim_start();
    let length = numeric
        .find(|character: char| {
            !(character.is_ascii_digit()
                || character == '-'
                || character == '+'
                || character == '.'
                || character == 'e'
                || character == 'E')
        })
        .unwrap_or(numeric.len());
    numeric[..length]
        .parse::<f64>()
        .map_err(|error| format!("invalid JSON number '{}': {error}", &numeric[..length]))
}

fn parse_string_after(input: &str, after: usize) -> Result<String, String> {
    let tail = input
        .get(after..)
        .ok_or_else(|| "invalid string offset".to_string())?;
    let colon = tail
        .find(':')
        .ok_or_else(|| "JSON string is missing ':'".to_string())?;
    let value = tail[colon + 1..].trim_start();
    if !value.starts_with('"') {
        return Err("JSON value is not a string".to_string());
    }
    let mut output = String::new();
    let mut escaped = false;
    for character in value[1..].chars() {
        if escaped {
            match character {
                'n' => output.push('\n'),
                'r' => output.push('\r'),
                't' => output.push('\t'),
                '"' => output.push('"'),
                '\\' => output.push('\\'),
                other => output.push(other),
            }
            escaped = false;
        } else if character == '\\' {
            escaped = true;
        } else if character == '"' {
            return Ok(output);
        } else {
            output.push(character);
        }
    }
    Err("unterminated JSON string".to_string())
}

fn validate_checkpoint_name(name: &str) -> Result<(), String> {
    if !name.starts_with("CP-") {
        return Err("checkpoint names must start with CP-".to_string());
    }
    if name.len() > 96
        || !name.chars().all(|character| {
            character.is_ascii_alphanumeric() || character == '-' || character == '_'
        })
    {
        return Err("checkpoint name contains unsafe characters".to_string());
    }
    Ok(())
}

fn current_git_commit(root: &Path) -> Option<String> {
    let output = Command::new("git")
        .arg("-C")
        .arg(root)
        .args(["rev-parse", "HEAD"])
        .output()
        .ok()?;
    if !output.status.success() {
        return None;
    }
    Some(String::from_utf8_lossy(&output.stdout).trim().to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn repository_root() -> PathBuf {
        Path::new(env!("CARGO_MANIFEST_DIR"))
            .join("../..")
            .canonicalize()
            .expect("repository root")
    }

    #[test]
    fn truth_score_is_deliberately_conservative() {
        let score = truth_score(&repository_root()).expect("truth score");
        assert!((score - 5.0).abs() < f64::EPSILON);
    }

    #[test]
    fn truth_score_binds_weight_and_completion_to_one_milestone() {
        let unique = format!(
            "icstudio-truth-test-{}-{}",
            std::process::id(),
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .expect("time")
                .as_nanos()
        );
        let root = std::env::temp_dir().join(unique);
        fs::create_dir_all(root.join(".project")).expect("project directory");
        fs::write(
            root.join(TRUTH_PATH),
            "{\"milestones\":[{\"id\":\"M0\",\"completion\":1,\"weight\":2},{\"id\":\"M1\",\"weight\":6,\"completion\":0.5}],\"reported_score\":5}",
        )
        .expect("truth fixture");
        assert!((truth_score(&root).expect("truth score") - 5.0).abs() < f64::EPSILON);
        fs::remove_dir_all(root).expect("cleanup");
    }

    #[test]
    fn project_state_validates() {
        validate_project_state(&repository_root()).expect("valid implementation state");
    }

    #[test]
    fn checkpoint_detects_state_drift_and_remains_immutable() {
        let unique = format!(
            "icstudio-checkpoint-test-{}-{}",
            std::process::id(),
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .expect("time")
                .as_nanos()
        );
        let root = std::env::temp_dir().join(unique);
        fs::create_dir_all(root.join(".project")).expect("project directory");
        fs::create_dir_all(root.join("toolchains")).expect("toolchain directory");
        for (_, manifest_path) in WORKSPACE_CRATES {
            let parent = root
                .join(manifest_path)
                .parent()
                .expect("manifest parent")
                .to_path_buf();
            fs::create_dir_all(parent).expect("crate directory");
        }

        for relative in ["AGENTS.md", "LICENSE"] {
            fs::write(root.join(relative), "test").expect("fixture");
        }
        fs::write(
            root.join("Cargo.toml"),
            "[workspace.package]\nlicense = \"MIT\"\n",
        )
        .expect("workspace fixture");
        for (_, manifest_path) in WORKSPACE_CRATES {
            fs::write(
                root.join(manifest_path),
                "[package]\nlicense.workspace = true\n",
            )
            .expect("crate fixture");
        }
        fs::write(
            root.join("Cargo.lock"),
            "version = 4\n[[package]]\nname = \"icstudio-platform\"\nversion = \"0.1.0\"\n",
        )
        .expect("lock fixture");
        fs::write(
            root.join(CAPABILITIES_PATH),
            REQUIRED_CAPABILITIES
                .iter()
                .map(|value| format!("{{\"id\":\"{value}\"}}"))
                .collect::<Vec<_>>()
                .join(","),
        )
        .expect("capabilities fixture");
        fs::write(
            root.join(TRUTH_PATH),
            "{\"milestones\":[{\"id\":\"M0\",\"weight\":2,\"completion\":1}],\"reported_score\":2}",
        )
        .expect("truth fixture");
        fs::write(root.join("toolchains/mcp.lock"), MCP_PROTOCOL_VERSION)
            .expect("protocol fixture");

        let checkpoint = create_checkpoint(&root, "CP-TEST").expect("checkpoint creation");
        let immutable_contents = fs::read_to_string(&checkpoint).expect("checkpoint contents");
        assert!(create_checkpoint(&root, "CP-TEST").is_err());
        assert_eq!(
            fs::read_to_string(&checkpoint).expect("checkpoint contents"),
            immutable_contents
        );
        resume_check(&root, "CP-TEST").expect("checkpoint should match");
        fs::write(
            root.join(TRUTH_PATH),
            "{\"milestones\":[{\"id\":\"M0\",\"weight\":2,\"completion\":0}],\"reported_score\":0}",
        )
        .expect("truth mutation");
        assert!(resume_check(&root, "CP-TEST").is_err());
        fs::remove_dir_all(root).expect("cleanup");
    }
}
