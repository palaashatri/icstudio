//! Deterministic circuit netlist IR and bounded SPICE parser scaffold.
//!
//! M1 supports structural parsing and canonicalization only. Values remain strings and no
//! device equations, units, model evaluation, or simulation semantics are claimed.

use std::collections::{BTreeMap, BTreeSet};

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DeviceKind {
    Resistor,
    Capacitor,
    VoltageSource,
    Mosfet,
    Subcircuit,
}

impl DeviceKind {
    fn as_str(&self) -> &'static str {
        match self {
            Self::Resistor => "resistor",
            Self::Capacitor => "capacitor",
            Self::VoltageSource => "voltage_source",
            Self::Mosfet => "mosfet",
            Self::Subcircuit => "subcircuit",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Instance {
    pub name: String,
    pub kind: DeviceKind,
    pub terminals: Vec<String>,
    pub model_or_value: String,
    pub parameters: BTreeMap<String, String>,
    pub source_line: usize,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Circuit {
    pub name: String,
    pub ports: Vec<String>,
    pub nets: BTreeSet<String>,
    pub parameters: BTreeMap<String, String>,
    pub instances: BTreeMap<String, Instance>,
}

impl Circuit {
    pub fn validate(&self) -> Result<(), String> {
        validate_name("circuit", &self.name)?;
        for port in &self.ports {
            validate_name("port", port)?;
            if !self.nets.contains(port) {
                return Err(format!("port '{port}' is not present in circuit nets"));
            }
        }
        for (name, instance) in &self.instances {
            if name != &instance.name {
                return Err(format!("instance map key '{name}' is inconsistent"));
            }
            validate_name("instance", &instance.name)?;
            if instance.terminals.is_empty() {
                return Err(format!("instance '{}' has no terminals", instance.name));
            }
            for net in &instance.terminals {
                if !self.nets.contains(net) {
                    return Err(format!(
                        "instance '{}' references missing net '{net}'",
                        instance.name
                    ));
                }
            }
        }
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Netlist {
    pub title: String,
    pub top: String,
    pub circuits: BTreeMap<String, Circuit>,
}

impl Netlist {
    pub fn validate(&self) -> Result<(), String> {
        if self.title.chars().any(char::is_control) {
            return Err("netlist title contains control characters".to_string());
        }
        if !self.circuits.contains_key(&self.top) {
            return Err(format!("top circuit '{}' does not exist", self.top));
        }
        for (name, circuit) in &self.circuits {
            if name != &circuit.name {
                return Err(format!("circuit map key '{name}' is inconsistent"));
            }
            circuit.validate()?;
        }
        Ok(())
    }
}

pub fn parse_spice(input: &str) -> Result<Netlist, String> {
    let mut title = String::new();
    let mut circuits = BTreeMap::new();
    let mut active: Option<Circuit> = None;
    let mut top = None;

    for (index, raw_line) in input.lines().enumerate() {
        let line_number = index + 1;
        let line = raw_line.trim();
        if line.is_empty() || line.starts_with('*') {
            continue;
        }
        if title.is_empty() && !line.starts_with('.') {
            title = line.to_string();
            continue;
        }
        let tokens: Vec<&str> = line.split_whitespace().collect();
        if tokens.is_empty() {
            continue;
        }
        if tokens[0].eq_ignore_ascii_case(".subckt") {
            if active.is_some() {
                return Err(format!("nested .subckt at line {line_number}"));
            }
            if tokens.len() < 2 {
                return Err(format!(".subckt requires a name at line {line_number}"));
            }
            let name = tokens[1].to_string();
            validate_name("circuit", &name)?;
            let ports = tokens[2..]
                .iter()
                .map(|token| (*token).to_string())
                .collect();
            let nets = tokens[2..]
                .iter()
                .map(|token| (*token).to_string())
                .collect();
            if top.is_none() {
                top = Some(name.clone());
            }
            active = Some(Circuit {
                name,
                ports,
                nets,
                parameters: BTreeMap::new(),
                instances: BTreeMap::new(),
            });
            continue;
        }
        if tokens[0].eq_ignore_ascii_case(".ends") {
            let circuit = active
                .take()
                .ok_or_else(|| format!(".ends without .subckt at line {line_number}"))?;
            let name = circuit.name.clone();
            if circuits.insert(name.clone(), circuit).is_some() {
                return Err(format!("duplicate circuit '{name}'"));
            }
            continue;
        }
        if tokens[0].eq_ignore_ascii_case(".param") {
            let circuit = active
                .as_mut()
                .ok_or_else(|| format!(".param outside .subckt at line {line_number}"))?;
            for assignment in &tokens[1..] {
                let (name, value) = assignment.split_once('=').ok_or_else(|| {
                    format!("invalid .param assignment '{assignment}' at line {line_number}")
                })?;
                validate_name("parameter", name)?;
                circuit
                    .parameters
                    .insert(name.to_string(), value.to_string());
            }
            continue;
        }
        if line.starts_with('.') {
            return Err(format!(
                "unsupported SPICE directive '{}' at line {line_number}",
                tokens[0]
            ));
        }
        let circuit = active
            .as_mut()
            .ok_or_else(|| format!("instance outside .subckt at line {line_number}"))?;
        let instance = parse_instance(&tokens, line_number)?;
        for net in &instance.terminals {
            circuit.nets.insert(net.clone());
        }
        if circuit
            .instances
            .insert(instance.name.clone(), instance)
            .is_some()
        {
            return Err(format!("duplicate instance at line {line_number}"));
        }
    }

    if let Some(circuit) = active {
        return Err(format!(
            "unterminated .subckt '{}': missing .ends",
            circuit.name
        ));
    }
    let top = top.ok_or_else(|| "SPICE input contains no .subckt".to_string())?;
    let netlist = Netlist {
        title,
        top,
        circuits,
    };
    netlist.validate()?;
    Ok(netlist)
}

fn parse_instance(tokens: &[&str], line_number: usize) -> Result<Instance, String> {
    let name = tokens[0].to_string();
    validate_name("instance", &name)?;
    let designator = name
        .chars()
        .next()
        .ok_or_else(|| format!("empty instance name at line {line_number}"))?
        .to_ascii_uppercase();
    let (kind, terminal_count, model_index) = match designator {
        'R' => (DeviceKind::Resistor, 2, 3),
        'C' => (DeviceKind::Capacitor, 2, 3),
        'V' => (DeviceKind::VoltageSource, 2, 3),
        'M' => (DeviceKind::Mosfet, 4, 5),
        'X' => {
            if tokens.len() < 4 {
                return Err(format!(
                    "subcircuit instance is incomplete at line {line_number}"
                ));
            }
            (DeviceKind::Subcircuit, tokens.len() - 2, tokens.len() - 1)
        }
        other => {
            return Err(format!(
                "unsupported SPICE device designator '{other}' at line {line_number}"
            ));
        }
    };
    if tokens.len() <= model_index || tokens.len() < terminal_count + 2 {
        return Err(format!(
            "instance '{name}' is incomplete at line {line_number}"
        ));
    }
    let terminals = tokens[1..=terminal_count]
        .iter()
        .map(|token| (*token).to_string())
        .collect();
    let model_or_value = tokens[model_index].to_string();
    let mut parameters = BTreeMap::new();
    for token in &tokens[model_index + 1..] {
        let (parameter, value) = token
            .split_once('=')
            .ok_or_else(|| format!("invalid instance parameter '{token}' at line {line_number}"))?;
        validate_name("parameter", parameter)?;
        parameters.insert(parameter.to_string(), value.to_string());
    }
    Ok(Instance {
        name,
        kind,
        terminals,
        model_or_value,
        parameters,
        source_line: line_number,
    })
}

pub fn canonical_text(netlist: &Netlist) -> Result<String, String> {
    netlist.validate()?;
    let mut output = format!("* {}\n", netlist.title);
    for circuit in netlist.circuits.values() {
        output.push_str(&format!(".subckt {}", circuit.name));
        for port in &circuit.ports {
            output.push(' ');
            output.push_str(port);
        }
        output.push('\n');
        if !circuit.parameters.is_empty() {
            output.push_str(".param");
            for (name, value) in &circuit.parameters {
                output.push_str(&format!(" {name}={value}"));
            }
            output.push('\n');
        }
        for instance in circuit.instances.values() {
            output.push_str(&instance.name);
            for terminal in &instance.terminals {
                output.push(' ');
                output.push_str(terminal);
            }
            output.push(' ');
            output.push_str(&instance.model_or_value);
            for (name, value) in &instance.parameters {
                output.push_str(&format!(" {name}={value}"));
            }
            output.push_str(&format!(" ; kind={}\n", instance.kind.as_str()));
        }
        output.push_str(&format!(".ends {}\n", circuit.name));
    }
    Ok(output)
}

fn validate_name(kind: &str, value: &str) -> Result<(), String> {
    if value.is_empty()
        || value.len() > 128
        || value
            .chars()
            .any(|character| character.is_whitespace() || character.is_control())
    {
        return Err(format!("{kind} name '{value}' is invalid"));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    const INVERTER: &str = "CMOS inverter\n.subckt inv in out vdd vss\n.param w=1u l=0.15u\nM1 out in vdd vdd pmos w=2u l=0.15u\nM2 out in vss vss nmos w=1u l=0.15u\n.ends inv\n";

    #[test]
    fn parses_structural_cmos_inverter() {
        let netlist = parse_spice(INVERTER).expect("parse inverter");
        let circuit = netlist.circuits.get("inv").expect("circuit");
        assert_eq!(netlist.top, "inv");
        assert_eq!(circuit.instances.len(), 2);
        assert_eq!(circuit.ports, ["in", "out", "vdd", "vss"]);
        assert_eq!(circuit.parameters.get("w").map(String::as_str), Some("1u"));
    }

    #[test]
    fn canonicalization_is_stable() {
        let first = canonical_text(&parse_spice(INVERTER).expect("parse")).expect("canonical");
        let second = canonical_text(&parse_spice(INVERTER).expect("parse")).expect("canonical");
        assert_eq!(first, second);
        assert!(first.contains("M1 out in vdd vdd pmos"));
    }

    #[test]
    fn malformed_and_unsupported_input_reports_source_line() {
        let error = parse_spice("title\n.subckt bad a b\nQ1 a b c model\n.ends bad\n")
            .expect_err("unsupported device");
        assert!(error.contains("line 3"));
        let error = parse_spice("title\n.subckt bad a b\nR1 a\n.ends bad\n")
            .expect_err("incomplete resistor");
        assert!(error.contains("line 3"));
    }
}
