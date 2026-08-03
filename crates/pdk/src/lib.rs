//! Deterministic, clean-room PDK metadata for the M1 headless kernel.
//!
//! This crate defines technology units, layer-purpose mappings, model references, and
//! reproducible serialization. It does not execute PCells, rule decks, or compact models.

use std::collections::{BTreeMap, BTreeSet};

const MAGIC: &str = "ICSTUDIO_PDK";
const VERSION: u32 = 1;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Layer {
    pub name: String,
    pub purpose: String,
    pub gds_layer: u16,
    pub gds_datatype: u16,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ModelReference {
    pub name: String,
    pub kind: String,
    pub relative_path: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Technology {
    pub schema_version: u32,
    pub name: String,
    pub dbu_per_micron: u32,
    pub layers: BTreeMap<String, Layer>,
    pub models: BTreeMap<String, ModelReference>,
}

impl Technology {
    pub fn new(name: impl Into<String>, dbu_per_micron: u32) -> Result<Self, String> {
        let name = name.into();
        validate_identifier("technology", &name)?;
        if dbu_per_micron == 0 {
            return Err("dbu_per_micron must be greater than zero".to_string());
        }
        Ok(Self {
            schema_version: VERSION,
            name,
            dbu_per_micron,
            layers: BTreeMap::new(),
            models: BTreeMap::new(),
        })
    }

    pub fn add_layer(&mut self, layer: Layer) -> Result<(), String> {
        validate_identifier("layer", &layer.name)?;
        validate_identifier("purpose", &layer.purpose)?;
        let key = layer_key(&layer.name, &layer.purpose);
        if self.layers.contains_key(&key) {
            return Err(format!(
                "layer-purpose '{}:{}' already exists",
                layer.name, layer.purpose
            ));
        }
        if self.layers.values().any(|existing| {
            existing.gds_layer == layer.gds_layer
                && existing.gds_datatype == layer.gds_datatype
        }) {
            return Err(format!(
                "GDS mapping {}/{} is already assigned",
                layer.gds_layer, layer.gds_datatype
            ));
        }
        self.layers.insert(key, layer);
        Ok(())
    }

    pub fn add_model(&mut self, model: ModelReference) -> Result<(), String> {
        validate_identifier("model", &model.name)?;
        validate_identifier("model kind", &model.kind)?;
        validate_relative_path(&model.relative_path)?;
        if self.models.insert(model.name.clone(), model).is_some() {
            return Err("model name already exists".to_string());
        }
        Ok(())
    }

    pub fn layer(&self, name: &str, purpose: &str) -> Option<&Layer> {
        self.layers.get(&layer_key(name, purpose))
    }

    pub fn validate(&self) -> Result<(), String> {
        if self.schema_version != VERSION {
            return Err(format!(
                "unsupported PDK schema version {}",
                self.schema_version
            ));
        }
        validate_identifier("technology", &self.name)?;
        if self.dbu_per_micron == 0 {
            return Err("dbu_per_micron must be greater than zero".to_string());
        }
        let mut mappings = BTreeSet::new();
        for (key, layer) in &self.layers {
            validate_identifier("layer", &layer.name)?;
            validate_identifier("purpose", &layer.purpose)?;
            if key != &layer_key(&layer.name, &layer.purpose) {
                return Err(format!("layer map key '{key}' is inconsistent"));
            }
            if !mappings.insert((layer.gds_layer, layer.gds_datatype)) {
                return Err(format!(
                    "duplicate GDS mapping {}/{}",
                    layer.gds_layer, layer.gds_datatype
                ));
            }
        }
        for (key, model) in &self.models {
            if key != &model.name {
                return Err(format!("model map key '{key}' is inconsistent"));
            }
            validate_identifier("model", &model.name)?;
            validate_identifier("model kind", &model.kind)?;
            validate_relative_path(&model.relative_path)?;
        }
        Ok(())
    }
}

pub fn serialize(technology: &Technology) -> Result<String, String> {
    technology.validate()?;
    let mut output = format!("{MAGIC}\t{VERSION}\n");
    output.push_str(&format!(
        "technology\t{}\t{}\n",
        escape_field(&technology.name),
        technology.dbu_per_micron
    ));
    for layer in technology.layers.values() {
        output.push_str(&format!(
            "layer\t{}\t{}\t{}\t{}\n",
            escape_field(&layer.name),
            escape_field(&layer.purpose),
            layer.gds_layer,
            layer.gds_datatype
        ));
    }
    for model in technology.models.values() {
        output.push_str(&format!(
            "model\t{}\t{}\t{}\n",
            escape_field(&model.name),
            escape_field(&model.kind),
            escape_field(&model.relative_path)
        ));
    }
    Ok(output)
}

pub fn deserialize(input: &str) -> Result<Technology, String> {
    let mut lines = input.lines();
    let header = lines
        .next()
        .ok_or_else(|| "PDK metadata is empty".to_string())?;
    if header != format!("{MAGIC}\t{VERSION}") {
        return Err(format!("unsupported PDK header '{header}'"));
    }
    let technology_line = lines
        .next()
        .ok_or_else(|| "technology record is missing".to_string())?;
    let fields: Vec<&str> = technology_line.split('\t').collect();
    if fields.len() != 3 || fields[0] != "technology" {
        return Err("invalid technology record".to_string());
    }
    let name = unescape_field(fields[1])?;
    let dbu_per_micron = fields[2]
        .parse::<u32>()
        .map_err(|error| format!("invalid dbu_per_micron '{}': {error}", fields[2]))?;
    let mut technology = Technology::new(name, dbu_per_micron)?;
    for (line_index, line) in lines.enumerate() {
        if line.is_empty() {
            continue;
        }
        let fields: Vec<&str> = line.split('\t').collect();
        match fields.first().copied() {
            Some("layer") if fields.len() == 5 => {
                technology.add_layer(Layer {
                    name: unescape_field(fields[1])?,
                    purpose: unescape_field(fields[2])?,
                    gds_layer: fields[3].parse::<u16>().map_err(|error| {
                        format!("invalid GDS layer '{}' at line {}: {error}", fields[3], line_index + 3)
                    })?,
                    gds_datatype: fields[4].parse::<u16>().map_err(|error| {
                        format!("invalid GDS datatype '{}' at line {}: {error}", fields[4], line_index + 3)
                    })?,
                })?;
            }
            Some("model") if fields.len() == 4 => {
                technology.add_model(ModelReference {
                    name: unescape_field(fields[1])?,
                    kind: unescape_field(fields[2])?,
                    relative_path: unescape_field(fields[3])?,
                })?;
            }
            _ => {
                return Err(format!(
                    "invalid PDK record at line {}: '{line}'",
                    line_index + 3
                ));
            }
        }
    }
    technology.validate()?;
    Ok(technology)
}

fn layer_key(name: &str, purpose: &str) -> String {
    format!("{name}\u{001f}{purpose}")
}

fn validate_identifier(kind: &str, value: &str) -> Result<(), String> {
    if value.is_empty() || value.len() > 128 || value.chars().any(char::is_control) {
        return Err(format!("{kind} must be 1 to 128 printable characters"));
    }
    Ok(())
}

fn validate_relative_path(path: &str) -> Result<(), String> {
    if path.is_empty()
        || path.starts_with('/')
        || path.starts_with('\\')
        || path.split(['/', '\\']).any(|segment| segment == "..")
        || path.chars().any(char::is_control)
    {
        return Err(format!("model path '{path}' is not a safe relative path"));
    }
    Ok(())
}

fn escape_field(value: &str) -> String {
    let mut output = String::with_capacity(value.len());
    for character in value.chars() {
        match character {
            '\\' => output.push_str("\\\\"),
            '\t' => output.push_str("\\t"),
            '\n' => output.push_str("\\n"),
            '\r' => output.push_str("\\r"),
            other => output.push(other),
        }
    }
    output
}

fn unescape_field(value: &str) -> Result<String, String> {
    let mut output = String::with_capacity(value.len());
    let mut characters = value.chars();
    while let Some(character) = characters.next() {
        if character != '\\' {
            output.push(character);
            continue;
        }
        let escaped = characters
            .next()
            .ok_or_else(|| "unterminated field escape".to_string())?;
        match escaped {
            '\\' => output.push('\\'),
            't' => output.push('\t'),
            'n' => output.push('\n'),
            'r' => output.push('\r'),
            other => return Err(format!("unsupported field escape '\\{other}'")),
        }
    }
    Ok(output)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn fixture() -> Technology {
        let mut technology = Technology::new("open_demo", 1000).expect("technology");
        technology
            .add_layer(Layer {
                name: "metal1".to_string(),
                purpose: "drawing".to_string(),
                gds_layer: 68,
                gds_datatype: 20,
            })
            .expect("layer");
        technology
            .add_layer(Layer {
                name: "metal1".to_string(),
                purpose: "pin".to_string(),
                gds_layer: 68,
                gds_datatype: 16,
            })
            .expect("pin layer");
        technology
            .add_model(ModelReference {
                name: "tt".to_string(),
                kind: "spice".to_string(),
                relative_path: "models/tt.spice".to_string(),
            })
            .expect("model");
        technology
    }

    #[test]
    fn deterministic_round_trip_preserves_layer_purposes() {
        let first = serialize(&fixture()).expect("serialize");
        let decoded = deserialize(&first).expect("deserialize");
        let second = serialize(&decoded).expect("serialize again");
        assert_eq!(first, second);
        assert_eq!(
            decoded.layer("metal1", "pin").expect("pin").gds_datatype,
            16
        );
    }

    #[test]
    fn duplicate_gds_mapping_is_rejected() {
        let mut technology = fixture();
        let error = technology
            .add_layer(Layer {
                name: "duplicate".to_string(),
                purpose: "drawing".to_string(),
                gds_layer: 68,
                gds_datatype: 20,
            })
            .expect_err("duplicate mapping");
        assert!(error.contains("already assigned"));
    }

    #[test]
    fn unsafe_model_paths_are_rejected() {
        let mut technology = Technology::new("demo", 1000).expect("technology");
        assert!(technology
            .add_model(ModelReference {
                name: "bad".to_string(),
                kind: "spice".to_string(),
                relative_path: "../secret".to_string(),
            })
            .is_err());
    }
}
