package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.icstudio.io.GdsRecordReader;
import java.io.IOException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Compares GDSII record framing, offsets, and prefix validation with the recorded Rust oracle. */
class FormatsOracleTest {
    @Test
    void everyCaseMatchesTheRustDump() throws IOException {
        for (String name : OracleCorpus.cases("formats", "input")) {
            byte[] input = HexFormat.of().parseHex(OracleCorpus.read("formats/" + name + ".input").strip());
            assertEquals(OracleCorpus.read("formats/" + name + ".expected"), dump(input), name);
        }
    }

    static String dump(byte[] input) {
        var output = new StringBuilder();
        try {
            output.append("ok\n");
            for (var record : GdsRecordReader.parse(input)) {
                output.append("record\t").append(record.offset())
                        .append('\t').append(String.format("%02x", record.recordType()))
                        .append('\t').append(String.format("%02x", record.dataType()))
                        .append('\t').append(HexFormat.of().formatHex(record.payload())).append('\n');
            }
        } catch (IllegalArgumentException error) {
            output.setLength(0);
            output.append("error\t").append(error.getMessage()).append('\n');
        }
        try {
            GdsRecordReader.validateLibraryPrefix(input);
            output.append("prefix\tok\n");
        } catch (IllegalArgumentException error) {
            output.append("prefix\terror\t").append(error.getMessage()).append('\n');
        }
        return output.toString();
    }
}
