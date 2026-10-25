package dev.icstudio.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResultCodecTest {
    @TempDir
    java.nio.file.Path temporary;

    static ResultSet fixture() {
        var manifest = new RunManifest("run-1", "0123456789abcdef0123456789abcdef", 3, "transient",
                "deadbeef", "reference", "0.1.0");
        return new ResultSet(manifest, List.of(new Signal("v(out)", "V",
                List.of(0.0, 1.0e-9, 2.0e-9), List.of(0.0, 0.7, 1.2))));
    }

    @Test
    void exactFloatRoundTripIsDeterministic() {
        var first = ResultCodec.serialize(fixture());
        var decoded = ResultCodec.deserialize(first);

        assertEquals(first, ResultCodec.serialize(decoded));
        assertEquals(fixture().signals(), decoded.signals());
    }

    @Test
    void signedZeroAndSubnormalsSurviveExactly() {
        var manifest = fixture().manifest();
        var set = new ResultSet(manifest, List.of(new Signal("i(vdd)", "A",
                List.of(-0.0, 5.0e-324), List.of(Double.MIN_VALUE, -0.0))));
        var decoded = ResultCodec.deserialize(ResultCodec.serialize(set));

        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(decoded.signals().get("i(vdd)").axis().get(0)));
    }

    @Test
    void mismatchedVectorsAreRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new ResultSet(fixture().manifest(), List.of(
                new Signal("v(out)", "V", List.of(0.0, 1.0), List.of(0.0)))));
        assertEquals("signal 'v(out)' axis/value length mismatch: 2 versus 1", error.getMessage());
    }

    @Test
    void saveAndOpenPreserveProvenanceAndSummary() throws Exception {
        ResultStore.save(temporary, fixture());
        var opened = ResultStore.open(temporary);

        assertEquals(fixture().summaryJson(), opened.summaryJson());
        assertEquals("{\"runId\":\"run-1\",\"projectId\":\"0123456789abcdef0123456789abcdef\",\"projectRevision\":3,"
                + "\"analysis\":\"transient\",\"solver\":\"reference\",\"signalCount\":1,\"sampleCount\":3}",
                opened.summaryJson());
    }
}
