//! Versioned deterministic control envelopes for isolated ICStudio workers.

pub const RPC_SCHEMA_VERSION: u32 = 1;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RequestEnvelope {
    pub request_id: String,
    pub project_id: String,
    pub expected_revision: u64,
    pub command: String,
    pub payload: String,
}

impl RequestEnvelope {
    pub fn encode(&self) -> Result<String, String> {
        validate_identifier("request_id", &self.request_id)?;
        validate_identifier("project_id", &self.project_id)?;
        validate_identifier("command", &self.command)?;
        Ok(format!(
            "request\t{}\t{}\t{}\t{}\t{}\t{}",
            RPC_SCHEMA_VERSION,
            escape_field(&self.request_id),
            escape_field(&self.project_id),
            self.expected_revision,
            escape_field(&self.command),
            escape_field(&self.payload)
        ))
    }

    pub fn decode(input: &str) -> Result<Self, String> {
        let fields: Vec<&str> = input.trim_end_matches(['\r', '\n']).split('\t').collect();
        if fields.len() != 7 || fields[0] != "request" {
            return Err("invalid worker request envelope".to_string());
        }
        let version = fields[1]
            .parse::<u32>()
            .map_err(|error| format!("invalid RPC schema version '{}': {error}", fields[1]))?;
        if version != RPC_SCHEMA_VERSION {
            return Err(format!(
                "unsupported RPC schema version {version}; expected {RPC_SCHEMA_VERSION}"
            ));
        }
        let envelope = Self {
            request_id: unescape_field(fields[2])?,
            project_id: unescape_field(fields[3])?,
            expected_revision: fields[4]
                .parse::<u64>()
                .map_err(|error| format!("invalid expected revision '{}': {error}", fields[4]))?,
            command: unescape_field(fields[5])?,
            payload: unescape_field(fields[6])?,
        };
        validate_identifier("request_id", &envelope.request_id)?;
        validate_identifier("project_id", &envelope.project_id)?;
        validate_identifier("command", &envelope.command)?;
        Ok(envelope)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResponseEnvelope {
    pub request_id: String,
    pub success: bool,
    pub payload: String,
}

impl ResponseEnvelope {
    pub fn encode(&self) -> Result<String, String> {
        validate_identifier("request_id", &self.request_id)?;
        Ok(format!(
            "response\t{}\t{}\t{}\t{}",
            RPC_SCHEMA_VERSION,
            escape_field(&self.request_id),
            if self.success { "ok" } else { "error" },
            escape_field(&self.payload)
        ))
    }

    pub fn decode(input: &str) -> Result<Self, String> {
        let fields: Vec<&str> = input.trim_end_matches(['\r', '\n']).split('\t').collect();
        if fields.len() != 5 || fields[0] != "response" {
            return Err("invalid worker response envelope".to_string());
        }
        let version = fields[1]
            .parse::<u32>()
            .map_err(|error| format!("invalid RPC schema version '{}': {error}", fields[1]))?;
        if version != RPC_SCHEMA_VERSION {
            return Err(format!(
                "unsupported RPC schema version {version}; expected {RPC_SCHEMA_VERSION}"
            ));
        }
        let success = match fields[3] {
            "ok" => true,
            "error" => false,
            value => return Err(format!("invalid worker response status '{value}'")),
        };
        let envelope = Self {
            request_id: unescape_field(fields[2])?,
            success,
            payload: unescape_field(fields[4])?,
        };
        validate_identifier("request_id", &envelope.request_id)?;
        Ok(envelope)
    }
}

fn validate_identifier(kind: &str, value: &str) -> Result<(), String> {
    if value.is_empty() || value.len() > 256 || value.chars().any(char::is_control) {
        return Err(format!("{kind} must be 1 to 256 printable characters"));
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
            .ok_or_else(|| "unterminated RPC field escape".to_string())?;
        match escaped {
            '\\' => output.push('\\'),
            't' => output.push('\t'),
            'n' => output.push('\n'),
            'r' => output.push('\r'),
            other => return Err(format!("unsupported RPC field escape '\\{other}'")),
        }
    }
    Ok(output)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn request_round_trip_preserves_revision_and_payload() {
        let request = RequestEnvelope {
            request_id: "request-1".to_string(),
            project_id: "0123456789abcdef0123456789abcdef".to_string(),
            expected_revision: 42,
            command: "worker.ping".to_string(),
            payload: "line one\nline two\tvalue".to_string(),
        };
        let encoded = request.encode().expect("encode request");
        assert_eq!(
            RequestEnvelope::decode(&encoded).expect("decode request"),
            request
        );
    }

    #[test]
    fn response_round_trip_preserves_error_state() {
        let response = ResponseEnvelope {
            request_id: "request-2".to_string(),
            success: false,
            payload: "worker rejected input".to_string(),
        };
        let encoded = response.encode().expect("encode response");
        assert_eq!(
            ResponseEnvelope::decode(&encoded).expect("decode response"),
            response
        );
    }

    #[test]
    fn unsupported_schema_version_is_rejected() {
        let error = RequestEnvelope::decode("request\t999\tr\tp\t0\tworker.ping\t")
            .expect_err("version must fail");
        assert!(error.contains("unsupported RPC schema version"));
    }
}
