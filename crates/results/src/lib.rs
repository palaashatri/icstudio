//! Deterministic result manifest and vector-store foundation.
//!
//! Floating-point samples are serialized by exact IEEE-754 bit pattern. M1 stores bounded
//! vectors and provenance only; plotting, streaming, compression, and solver integration
//! remain future capabilities.

use std::collections::BTreeMap;
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};

const MAGIC: &str = "ICSTUDIO_RESULTS";
const VERSION: u32 = 1;
const MANIFEST: &str = "manifest.icres";

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RunManifest {
    pub run_id: String,
    pub project_id: String,
    pub project_revision: u64,
    pub analysis: String,
    pub input_hash: String,
    pub solver: String,
    pub solver_version: String,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Signal {
    pub name: String,
    pub unit: String,
    pub axis: Vec<f64>,
    pub values: Vec<f64>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ResultSet {
    pub manifest: RunManifest,
    pub signals: BTreeMap<String, Signal>,
}

impl ResultSet {
    pub fn validate(&self) -> Result<(), String> {
        validate_token("run_id", &self.manifest.run_id)?;
        validate_token("project_id", &self.manifest.project_id)?;
        validate_token("analysis", &self.manifest.analysis)?;
        validate_token("input_hash", &self.manifest.input_hash)?;
        validate_token("solver", &self.manifest.solver)?;
        validate_token("solver_version", &self.manifest.solver_version)?;
        for (name, signal) in &self.signals {
            if name != &signal.name {
                return Err(format!("signal map key '{name}' is inconsistent"));
            }
            validate_token("signal", &signal.name)?;
            validate_token("unit", &signal.unit)?;
            if signal.axis.len() != signal.values.len() {
                return Err(format!(
                    "signal '{}' axis/value length mismatch: {} versus {}",
                    signal.name,
                    signal.axis.len(),
                    signal.values.len()
                ));
            }
            if signal.axis.windows(2).any(|pair| pair[0] > pair[1]) {
                return Err(format!("signal '{}' axis is not monotonic", signal.name));
            }
            if signal.axis.len() > 10_000_000 {
                return Err(format!("signal '{}' exceeds the M1 sample limit", signal.name));
            }
        }
        Ok(())
    }

    pub fn summary_json(&self) -> Result<String, String> {
        self.validate()?;
        let samples: usize = self.signals.values().map(|signal| signal.values.len()).sum();
        Ok(format!(
            "{{\"runId\":\"{}\",\"projectId\":\"{}\",\"projectRevision\":{},\"analysis\":\"{}\",\"solver\":\"{}\",\"signalCount\":{},\"sampleCount\":{}}}",
            escape_json(&self.manifest.run_id),
            escape_json(&self.manifest.project_id),
            self.manifest.project_revision,
            escape_json(&self.manifest.analysis),
            escape_json(&self.manifest.solver),
            self.signals.len(),
            samples
        ))
    }
}

pub fn save(root: &Path, results: &ResultSet) -> Result<PathBuf, String> {
    results.validate()?;
    fs::create_dir_all(root)
        .map_err(|error| format!("failed to create {}: {error}", root.display()))?;
    let destination = root.join(MANIFEST);
    let temporary = root.join(format!("{MANIFEST}.tmp"));
    let mut file = fs::File::create(&temporary)
        .map_err(|error| format!("failed to create {}: {error}", temporary.display()))?;
    file.write_all(serialize(results)?.as_bytes())
        .map_err(|error| format!("failed to write {}: {error}", temporary.display()))?;
    file.sync_all()
        .map_err(|error| format!("failed to sync {}: {error}", temporary.display()))?;
    if destination.exists() {
        fs::remove_file(&destination)
            .map_err(|error| format!("failed to replace {}: {error}", destination.display()))?;
    }
    fs::rename(&temporary, &destination).map_err(|error| {
        format!(
            "failed to publish {} from {}: {error}",
            destination.display(),
            temporary.display()
        )
    })?;
    Ok(destination)
}

pub fn open(root: &Path) -> Result<ResultSet, String> {
    let path = root.join(MANIFEST);
    let input = fs::read_to_string(&path)
        .map_err(|error| format!("failed to read {}: {error}", path.display()))?;
    deserialize(&input)
}

pub fn serialize(results: &ResultSet) -> Result<String, String> {
    results.validate()?;
    let manifest = &results.manifest;
    let mut output = format!("{MAGIC}\t{VERSION}\n");
    output.push_str(&format!(
        "run\t{}\t{}\t{}\t{}\t{}\t{}\t{}\n",
        escape_field(&manifest.run_id),
        escape_field(&manifest.project_id),
        manifest.project_revision,
        escape_field(&manifest.analysis),
        escape_field(&manifest.input_hash),
        escape_field(&manifest.solver),
        escape_field(&manifest.solver_version)
    ));
    for signal in results.signals.values() {
        output.push_str(&format!(
            "signal\t{}\t{}\t{}\n",
            escape_field(&signal.name),
            escape_field(&signal.unit),
            signal.values.len()
        ));
        for (axis, value) in signal.axis.iter().zip(&signal.values) {
            output.push_str(&format!(
                "sample\t{:016x}\t{:016x}\n",
                axis.to_bits(),
                value.to_bits()
            ));
        }
    }
    Ok(output)
}

pub fn deserialize(input: &str) -> Result<ResultSet, String> {
    let mut lines = input.lines();
    let header = lines
        .next()
        .ok_or_else(|| "result database is empty".to_string())?;
    if header != format!("{MAGIC}\t{VERSION}") {
        return Err(format!("unsupported result header '{header}'"));
    }
    let run = lines
        .next()
        .ok_or_else(|| "result run record is missing".to_string())?;
    let fields: Vec<&str> = run.split('\t').collect();
    if fields.len() != 8 || fields[0] != "run" {
        return Err("invalid result run record".to_string());
    }
    let manifest = RunManifest {
        run_id: unescape_field(fields[1])?,
        project_id: unescape_field(fields[2])?,
        project_revision: fields[3]
            .parse::<u64>()
            .map_err(|error| format!("invalid project revision '{}': {error}", fields[3]))?,
        analysis: unescape_field(fields[4])?,
        input_hash: unescape_field(fields[5])?,
        solver: unescape_field(fields[6])?,
        solver_version: unescape_field(fields[7])?,
    };
    let mut signals = BTreeMap::new();
    let all_lines: Vec<&str> = lines.collect();
    let mut cursor = 0;
    while cursor < all_lines.len() {
        let fields: Vec<&str> = all_lines[cursor].split('\t').collect();
        if fields.len() != 4 || fields[0] != "signal" {
            return Err(format!("invalid signal record at line {}", cursor + 3));
        }
        let name = unescape_field(fields[1])?;
        let unit = unescape_field(fields[2])?;
        let count = fields[3]
            .parse::<usize>()
            .map_err(|error| format!("invalid signal sample count '{}': {error}", fields[3]))?;
        cursor += 1;
        let mut axis = Vec::with_capacity(count);
        let mut values = Vec::with_capacity(count);
        for _ in 0..count {
            let line = all_lines
                .get(cursor)
                .ok_or_else(|| format!("signal '{name}' is truncated"))?;
            let fields: Vec<&str> = line.split('\t').collect();
            if fields.len() != 3 || fields[0] != "sample" {
                return Err(format!("invalid sample record at line {}", cursor + 3));
            }
            axis.push(f64::from_bits(parse_bits(fields[1])?));
            values.push(f64::from_bits(parse_bits(fields[2])?));
            cursor += 1;
        }
        let signal = Signal {
            name: name.clone(),
            unit,
            axis,
            values,
        };
        if signals.insert(name.clone(), signal).is_some() {
            return Err(format!("duplicate signal '{name}'"));
        }
    }
    let results = ResultSet { manifest, signals };
    results.validate()?;
    Ok(results)
}

fn parse_bits(value: &str) -> Result<u64, String> {
    if value.len() != 16 || !value.bytes().all(|byte| byte.is_ascii_hexdigit()) {
        return Err(format!("invalid IEEE-754 bit pattern '{value}'"));
    }
    u64::from_str_radix(value, 16)
        .map_err(|error| format!("invalid IEEE-754 bit pattern '{value}': {error}"))
}

fn validate_token(kind: &str, value: &str) -> Result<(), String> {
    if value.is_empty() || value.len() > 256 || value.chars().any(char::is_control) {
        return Err(format!("{kind} must be 1 to 256 printable characters"));
    }
    Ok(())
}

fn escape_field(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('\t', "\\t")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
}

fn unescape_field(value: &str) -> Result<String, String> {
    let mut output = String::new();
    let mut characters = value.chars();
    while let Some(character) = characters.next() {
        if character != '\\' {
            output.push(character);
            continue;
        }
        match characters
            .next()
            .ok_or_else(|| "unterminated result field escape".to_string())?
        {
            '\\' => output.push('\\'),
            't' => output.push('\t'),
            'n' => output.push('\n'),
            'r' => output.push('\r'),
            other => return Err(format!("unsupported result field escape '\\{other}'")),
        }
    }
    Ok(output)
}

fn escape_json(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
        .replace('\t', "\\t")
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::{SystemTime, UNIX_EPOCH};

    fn fixture() -> ResultSet {
        let mut signals = BTreeMap::new();
        signals.insert(
            "v(out)".to_string(),
            Signal {
                name: "v(out)".to_string(),
                unit: "V".to_string(),
                axis: vec![0.0, 1.0e-9, 2.0e-9],
                values: vec![0.0, 0.7, 1.2],
            },
        );
        ResultSet {
            manifest: RunManifest {
                run_id: "run-1".to_string(),
                project_id: "0123456789abcdef0123456789abcdef".to_string(),
                project_revision: 3,
                analysis: "transient".to_string(),
                input_hash: "deadbeef".to_string(),
                solver: "reference".to_string(),
                solver_version: "0.1.0".to_string(),
            },
            signals,
        }
    }

    #[test]
    fn exact_float_round_trip_is_deterministic() {
        let first = serialize(&fixture()).expect("serialize");
        let decoded = deserialize(&first).expect("deserialize");
        let second = serialize(&decoded).expect("serialize again");
        assert_eq!(first, second);
        assert_eq!(decoded, fixture());
    }

    #[test]
    fn save_open_and_summary_preserve_provenance() {
        let unique = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("time")
            .as_nanos();
        let root = std::env::temp_dir().join(format!("icstudio-results-{unique}"));
        save(&root, &fixture()).expect("save");
        let opened = open(&root).expect("open");
        let summary = opened.summary_json().expect("summary");
        assert!(summary.contains("\"projectRevision\":3"));
        assert!(summary.contains("\"sampleCount\":3"));
        fs::remove_dir_all(root).expect("cleanup");
    }

    #[test]
    fn mismatched_or_non_monotonic_vectors_are_rejected() {
        let mut results = fixture();
        results.signals.get_mut("v(out)").expect("signal").values.pop();
        assert!(results.validate().is_err());
        let mut results = fixture();
        results.signals.get_mut("v(out)").expect("signal").axis = vec![0.0, 2.0, 1.0];
        assert!(results.validate().is_err());
    }
}
