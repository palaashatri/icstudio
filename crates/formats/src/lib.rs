//! Safe parser scaffolds for physical-design interchange formats.
//!
//! M1 decodes and validates GDSII record framing only. It does not yet interpret hierarchy,
//! geometry, properties, units, or write GDSII output.

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct GdsRecord<'a> {
    pub offset: usize,
    pub record_type: u8,
    pub data_type: u8,
    pub payload: &'a [u8],
}

pub fn parse_gds_records(input: &[u8]) -> Result<Vec<GdsRecord<'_>>, String> {
    const MAX_RECORDS: usize = 10_000_000;
    let mut records = Vec::new();
    let mut offset = 0;
    while offset < input.len() {
        if records.len() >= MAX_RECORDS {
            return Err("GDSII record count exceeds the M1 safety limit".to_string());
        }
        let header = input
            .get(offset..offset + 4)
            .ok_or_else(|| format!("truncated GDSII record header at byte {offset}"))?;
        let length = usize::from(u16::from_be_bytes([header[0], header[1]]));
        if length < 4 {
            return Err(format!(
                "invalid GDSII record length {length} at byte {offset}: minimum is 4"
            ));
        }
        if length % 2 != 0 {
            return Err(format!(
                "invalid odd GDSII record length {length} at byte {offset}"
            ));
        }
        let end = offset
            .checked_add(length)
            .ok_or_else(|| format!("GDSII record length overflow at byte {offset}"))?;
        let record = input
            .get(offset..end)
            .ok_or_else(|| format!("truncated GDSII record at byte {offset}: needs {length} bytes"))?;
        records.push(GdsRecord {
            offset,
            record_type: record[2],
            data_type: record[3],
            payload: &record[4..],
        });
        offset = end;
    }
    Ok(records)
}

pub fn validate_gds_library_prefix(input: &[u8]) -> Result<(), String> {
    let records = parse_gds_records(input)?;
    let header = records
        .first()
        .ok_or_else(|| "GDSII stream contains no records".to_string())?;
    if header.record_type != 0x00 || header.data_type != 0x02 || header.payload.len() != 2 {
        return Err(format!(
            "GDSII stream must begin with HEADER/int2 payload, found type 0x{:02x}/0x{:02x}",
            header.record_type, header.data_type
        ));
    }
    let version = u16::from_be_bytes([header.payload[0], header.payload[1]]);
    if version == 0 {
        return Err("GDSII HEADER version must be non-zero".to_string());
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_framed_records_and_preserves_offsets() {
        let bytes = [
            0x00, 0x06, 0x00, 0x02, 0x02, 0x58, // HEADER version 600
            0x00, 0x04, 0x04, 0x00, // ENDLIB/no-data placeholder for framing
        ];
        let records = parse_gds_records(&bytes).expect("records");
        assert_eq!(records.len(), 2);
        assert_eq!(records[0].offset, 0);
        assert_eq!(records[0].payload, [0x02, 0x58]);
        assert_eq!(records[1].offset, 6);
        validate_gds_library_prefix(&bytes).expect("valid prefix");
    }

    #[test]
    fn rejects_short_odd_and_truncated_records_with_offsets() {
        let error = parse_gds_records(&[0x00, 0x02, 0x00, 0x00])
            .expect_err("short record must fail");
        assert!(error.contains("byte 0"));
        let error = parse_gds_records(&[0x00, 0x05, 0x00, 0x00, 0x00])
            .expect_err("odd record must fail");
        assert!(error.contains("odd"));
        let error = parse_gds_records(&[0x00, 0x08, 0x00, 0x00, 0x00, 0x00])
            .expect_err("truncated record must fail");
        assert!(error.contains("needs 8 bytes"));
    }

    #[test]
    fn rejects_stream_without_a_valid_header_record() {
        let bytes = [0x00, 0x04, 0x01, 0x00];
        let error = validate_gds_library_prefix(&bytes).expect_err("header must fail");
        assert!(error.contains("must begin with HEADER"));
    }
}
