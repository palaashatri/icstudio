package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.netlist.SpiceParser;
import dev.icstudio.netlist.SpiceWriter;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** Compares the structural SPICE parser and canonical writer with the recorded Rust oracle. */
class NetlistOracleTest {
    @Test
    void everyCaseMatchesTheRustOutcome() throws IOException {
        for (String name : OracleCorpus.cases("netlist/cases", "input")) {
            String input = OracleCorpus.read("netlist/cases/" + name + ".input");
            if (Files.exists(OracleCorpus.resolve("netlist/cases/" + name + ".error"))) {
                var error = assertThrows(IllegalArgumentException.class,
                        () -> SpiceWriter.canonicalText(SpiceParser.parse(input)), name);
                assertEquals(OracleCorpus.read("netlist/cases/" + name + ".error"), error.getMessage(), name);
            } else {
                assertEquals(OracleCorpus.read("netlist/cases/" + name + ".expected"),
                        SpiceWriter.canonicalText(SpiceParser.parse(input)), name);
            }
        }
    }
}
