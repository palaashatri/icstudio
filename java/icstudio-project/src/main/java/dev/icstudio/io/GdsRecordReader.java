package dev.icstudio.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bounded GDSII record-framing reader.
 *
 * <p>M1 validates record framing and the HEADER prefix only. It does not interpret hierarchy,
 * geometry, properties or units, and it does not write GDSII output.</p>
 */
public final class GdsRecordReader {
    private static final int MAX_RECORDS = 10_000_000;

    /** One framed record; {@code payload} is a defensive copy of the record body. */
    public record Record(int offset, int recordType, int dataType, byte[] payload) {
        public Record {
            payload = payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    private GdsRecordReader() {
    }

    public static List<Record> parse(byte[] input) {
        var records = new ArrayList<Record>();
        int offset = 0;
        while (offset < input.length) {
            if (records.size() >= MAX_RECORDS) {
                throw new IllegalArgumentException("GDSII record count exceeds the M1 safety limit");
            }
            if (input.length - offset < 4) {
                throw new IllegalArgumentException("truncated GDSII record header at byte " + offset);
            }
            int length = ((input[offset] & 0xFF) << 8) | (input[offset + 1] & 0xFF);
            if (length < 4) {
                throw new IllegalArgumentException(
                        "invalid GDSII record length " + length + " at byte " + offset + ": minimum is 4");
            }
            if (length % 2 != 0) {
                throw new IllegalArgumentException(
                        "invalid odd GDSII record length " + length + " at byte " + offset);
            }
            if (input.length - offset < length) {
                throw new IllegalArgumentException(
                        "truncated GDSII record at byte " + offset + ": needs " + length + " bytes");
            }
            records.add(new Record(
                    offset,
                    input[offset + 2] & 0xFF,
                    input[offset + 3] & 0xFF,
                    Arrays.copyOfRange(input, offset + 4, offset + length)));
            offset += length;
        }
        return List.copyOf(records);
    }

    public static void validateLibraryPrefix(byte[] input) {
        var records = parse(input);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("GDSII stream contains no records");
        }
        var header = records.getFirst();
        if (header.recordType() != 0x00 || header.dataType() != 0x02 || header.payload().length != 2) {
            throw new IllegalArgumentException(String.format(
                    "GDSII stream must begin with HEADER/int2 payload, found type 0x%02x/0x%02x",
                    header.recordType(),
                    header.dataType()));
        }
        var payload = header.payload();
        int version = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        if (version == 0) {
            throw new IllegalArgumentException("GDSII HEADER version must be non-zero");
        }
    }
}
