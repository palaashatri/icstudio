package dev.icstudio.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GdsRecordReaderTest {
    @Test
    void parsesFramedRecordsAndPreservesOffsets() {
        byte[] bytes = {0x00, 0x06, 0x00, 0x02, 0x02, 0x58, 0x00, 0x04, 0x04, 0x00};
        var records = GdsRecordReader.parse(bytes);

        assertEquals(2, records.size());
        assertEquals(6, records.get(1).offset());
        GdsRecordReader.validateLibraryPrefix(bytes);
    }

    @Test
    void rejectsShortOddAndTruncatedRecordsWithOffsets() {
        assertEquals("invalid GDSII record length 2 at byte 0: minimum is 4",
                assertThrows(IllegalArgumentException.class,
                        () -> GdsRecordReader.parse(new byte[] {0x00, 0x02, 0x00, 0x00})).getMessage());
        assertEquals("invalid odd GDSII record length 5 at byte 0",
                assertThrows(IllegalArgumentException.class,
                        () -> GdsRecordReader.parse(new byte[] {0x00, 0x05, 0x00, 0x00, 0x00})).getMessage());
        assertEquals("truncated GDSII record at byte 0: needs 8 bytes",
                assertThrows(IllegalArgumentException.class,
                        () -> GdsRecordReader.parse(new byte[] {0x00, 0x08, 0x00, 0x00, 0x00, 0x00})).getMessage());
    }
}
