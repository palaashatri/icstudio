package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.results.ResultCodec;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** Compares result manifests, exact-bit samples, and rejections with the recorded Rust oracle. */
class ResultsOracleTest {
    @Test
    void recordedManifestsRoundTripByteForByte() throws IOException {
        for (String name : new String[] {"fixture", "special-values"}) {
            String recorded = OracleCorpus.read("results/" + name + ".icres");
            assertEquals(recorded, ResultCodec.serialize(ResultCodec.deserialize(recorded)), name);
        }
    }

    @Test
    void summariesMatchTheRustSummaries() throws IOException {
        assertEquals(OracleCorpus.read("results/fixture-summary.json"),
                ResultCodec.deserialize(OracleCorpus.read("results/fixture.icres")).summaryJson());
        assertEquals(OracleCorpus.read("results/special-values-summary.json"),
                ResultCodec.deserialize(OracleCorpus.read("results/special-values.icres")).summaryJson());
    }

    @Test
    void everyCaseMatchesTheRustOutcome() throws IOException {
        for (String name : OracleCorpus.cases("results/cases", "input")) {
            String input = OracleCorpus.read("results/cases/" + name + ".input");
            if (Files.exists(OracleCorpus.resolve("results/cases/" + name + ".error"))) {
                var error = assertThrows(IllegalArgumentException.class,
                        () -> ResultCodec.serialize(ResultCodec.deserialize(input)), name);
                assertEquals(OracleCorpus.read("results/cases/" + name + ".error"), error.getMessage(), name);
            } else {
                assertEquals(OracleCorpus.read("results/cases/" + name + ".expected"),
                        ResultCodec.serialize(ResultCodec.deserialize(input)), name);
            }
        }
    }
}
