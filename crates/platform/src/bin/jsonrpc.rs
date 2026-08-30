use std::collections::BTreeMap;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Object {
    fields: BTreeMap<String, String>,
}

impl Object {
    pub fn parse(input: &str) -> Result<Self, String> {
        let bytes = input.as_bytes();
        let mut cursor = skip_whitespace(bytes, 0);
        if bytes.get(cursor) != Some(&b'{') {
            return Err("JSON value is not an object".to_string());
        }
        cursor += 1;
        let mut fields = BTreeMap::new();
        loop {
            cursor = skip_whitespace(bytes, cursor);
            if bytes.get(cursor) == Some(&b'}') {
                cursor += 1;
                break;
            }
            let (key, next) = parse_string(input, cursor)?;
            cursor = skip_whitespace(bytes, next);
            if bytes.get(cursor) != Some(&b':') {
                return Err(format!("JSON field '{key}' is missing ':'"));
            }
            cursor = skip_whitespace(bytes, cursor + 1);
            let end = value_end(input, cursor)?;
            let raw = input[cursor..end].trim().to_string();
            if raw.is_empty() {
                return Err(format!("JSON field '{key}' has no value"));
            }
            if fields.insert(key.clone(), raw).is_some() {
                return Err(format!("duplicate JSON field '{key}'"));
            }
            cursor = skip_whitespace(bytes, end);
            match bytes.get(cursor) {
                Some(b',') => cursor += 1,
                Some(b'}') => {
                    cursor += 1;
                    break;
                }
                _ => return Err("JSON object is missing ',' or '}'".to_string()),
            }
        }
        if skip_whitespace(bytes, cursor) != bytes.len() {
            return Err("trailing data after JSON object".to_string());
        }
        Ok(Self { fields })
    }

    pub fn empty() -> Self {
        Self {
            fields: BTreeMap::new(),
        }
    }

    pub fn raw(&self, key: &str) -> Option<&str> {
        self.fields.get(key).map(String::as_str)
    }

    pub fn string(&self, key: &str) -> Result<Option<String>, String> {
        match self.raw(key) {
            Some(raw) => {
                let (value, end) = parse_string(raw, 0)?;
                if skip_whitespace(raw.as_bytes(), end) != raw.len() {
                    return Err(format!("JSON field '{key}' is not a string"));
                }
                Ok(Some(value))
            }
            None => Ok(None),
        }
    }

    pub fn required_string(&self, key: &str) -> Result<String, String> {
        self.string(key)?
            .ok_or_else(|| format!("missing JSON string field '{key}'"))
    }

    pub fn object(&self, key: &str) -> Result<Option<Self>, String> {
        match self.raw(key) {
            Some(raw) => Self::parse(raw).map(Some),
            None => Ok(None),
        }
    }

    pub fn keys(&self) -> impl Iterator<Item = &str> {
        self.fields.keys().map(String::as_str)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Request {
    pub id: Option<String>,
    pub method: String,
    pub params: Object,
}

impl Request {
    pub fn parse(input: &str) -> Result<Self, String> {
        let object = Object::parse(input)?;
        for key in object.keys() {
            if !matches!(key, "jsonrpc" | "id" | "method" | "params") {
                return Err(format!("unsupported top-level JSON-RPC field '{key}'"));
            }
        }
        let version = object.required_string("jsonrpc")?;
        if version != "2.0" {
            return Err(format!("unsupported JSON-RPC version '{version}'"));
        }
        let method = object.required_string("method")?;
        let id = object.raw("id").map(validate_id).transpose()?;
        let params = object.object("params")?.unwrap_or_else(Object::empty);
        Ok(Self { id, method, params })
    }
}

fn validate_id(raw: &str) -> Result<String, String> {
    let raw = raw.trim();
    if raw.starts_with('"') {
        let (_, end) = parse_string(raw, 0)?;
        if skip_whitespace(raw.as_bytes(), end) != raw.len() {
            return Err("JSON-RPC id is not a string or integer".to_string());
        }
        return Ok(raw.to_string());
    }
    if raw == "null"
        || raw
            .chars()
            .any(|character| matches!(character, '.' | 'e' | 'E'))
        || raw.parse::<i64>().is_err()
    {
        return Err("JSON-RPC id must be a string or integer".to_string());
    }
    Ok(raw.to_string())
}

fn value_end(input: &str, start: usize) -> Result<usize, String> {
    let bytes = input.as_bytes();
    match bytes.get(start) {
        Some(b'"') => parse_string(input, start).map(|(_, end)| end),
        Some(b'{') | Some(b'[') => composite_end(input, start),
        Some(_) => {
            let mut cursor = start;
            while let Some(byte) = bytes.get(cursor).copied() {
                if matches!(byte, b',' | b'}') || byte.is_ascii_whitespace() {
                    break;
                }
                cursor += 1;
            }
            Ok(cursor)
        }
        None => Err("unexpected end of JSON input".to_string()),
    }
}

fn composite_end(input: &str, start: usize) -> Result<usize, String> {
    let bytes = input.as_bytes();
    let mut stack = vec![bytes[start]];
    let mut cursor = start + 1;
    let mut in_string = false;
    let mut escaped = false;
    while let Some(byte) = bytes.get(cursor).copied() {
        if in_string {
            if escaped {
                escaped = false;
            } else if byte == b'\\' {
                escaped = true;
            } else if byte == b'"' {
                in_string = false;
            }
            cursor += 1;
            continue;
        }
        match byte {
            b'"' => in_string = true,
            b'{' | b'[' => stack.push(byte),
            b'}' => {
                if stack.pop() != Some(b'{') {
                    return Err("mismatched JSON object delimiter".to_string());
                }
                if stack.is_empty() {
                    return Ok(cursor + 1);
                }
            }
            b']' => {
                if stack.pop() != Some(b'[') {
                    return Err("mismatched JSON array delimiter".to_string());
                }
                if stack.is_empty() {
                    return Ok(cursor + 1);
                }
            }
            _ => {}
        }
        cursor += 1;
    }
    Err("unterminated JSON object or array".to_string())
}

fn parse_string(input: &str, start: usize) -> Result<(String, usize), String> {
    let bytes = input.as_bytes();
    if bytes.get(start) != Some(&b'"') {
        return Err("JSON value is not a string".to_string());
    }
    let mut output = String::new();
    let mut cursor = start + 1;
    while let Some(byte) = bytes.get(cursor).copied() {
        match byte {
            b'"' => return Ok((output, cursor + 1)),
            b'\\' => {
                cursor += 1;
                let escaped = bytes
                    .get(cursor)
                    .copied()
                    .ok_or_else(|| "unterminated JSON string escape".to_string())?;
                match escaped {
                    b'"' => output.push('"'),
                    b'\\' => output.push('\\'),
                    b'/' => output.push('/'),
                    b'b' => output.push('\u{0008}'),
                    b'f' => output.push('\u{000c}'),
                    b'n' => output.push('\n'),
                    b'r' => output.push('\r'),
                    b't' => output.push('\t'),
                    b'u' => {
                        let end = cursor + 5;
                        let digits = input
                            .get(cursor + 1..end)
                            .ok_or_else(|| "incomplete JSON unicode escape".to_string())?;
                        let value = u16::from_str_radix(digits, 16)
                            .map_err(|error| format!("invalid JSON unicode escape: {error}"))?;
                        let character = char::from_u32(u32::from(value))
                            .ok_or_else(|| "invalid JSON unicode scalar".to_string())?;
                        output.push(character);
                        cursor = end - 1;
                    }
                    _ => return Err("unsupported JSON string escape".to_string()),
                }
            }
            value if value < 0x20 => return Err("control character in JSON string".to_string()),
            _ => {
                let tail = &input[cursor..];
                let character = tail
                    .chars()
                    .next()
                    .ok_or_else(|| "invalid UTF-8 JSON string".to_string())?;
                output.push(character);
                cursor += character.len_utf8() - 1;
            }
        }
        cursor += 1;
    }
    Err("unterminated JSON string".to_string())
}

fn skip_whitespace(bytes: &[u8], mut cursor: usize) -> usize {
    while bytes.get(cursor).is_some_and(u8::is_ascii_whitespace) {
        cursor += 1;
    }
    cursor
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn top_level_fields_are_not_confused_with_nested_fields() {
        let request = Request::parse(
            r#"{"jsonrpc":"2.0","params":{"id":1,"method":"wrong","name":"nested"},"id":7,"method":"ping"}"#,
        )
        .expect("parse request");
        assert_eq!(request.id.as_deref(), Some("7"));
        assert_eq!(request.method, "ping");
        assert_eq!(
            request.params.required_string("method").expect("method"),
            "wrong"
        );
    }

    #[test]
    fn notification_has_no_response_id() {
        let request =
            Request::parse(r#"{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}"#)
                .expect("parse notification");
        assert_eq!(request.id, None);
    }

    #[test]
    fn duplicate_and_unknown_top_level_fields_are_rejected() {
        assert!(Request::parse(r#"{"jsonrpc":"2.0","id":1,"id":2,"method":"ping"}"#).is_err());
        assert!(
            Request::parse(r#"{"jsonrpc":"2.0","id":1,"method":"ping","unexpected":true}"#)
                .is_err()
        );
    }
}
