package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.pdk.Layer;
import dev.icstudio.pdk.ModelReference;
import dev.icstudio.pdk.PdkCodec;
import dev.icstudio.pdk.Technology;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Compares PDK metadata round trips and rejections with the recorded Rust oracle. */
class PdkOracleTest {
    @Test
    void openDemoTechnologyMatchesTheRustBytes() throws IOException {
        var technology = Technology.create("open_demo", 1000);
        technology.addLayer(new Layer("metal1", "drawing", 68, 20));
        technology.addLayer(new Layer("metal1", "pin", 68, 16));
        technology.addModel(new ModelReference("tt", "spice", "models/tt.spice"));

        assertEquals(OracleCorpus.read("pdk/open-demo.pdk"), PdkCodec.serialize(technology));
    }

    @Test
    void everyCaseMatchesTheRustOutcome() throws IOException {
        for (String name : OracleCorpus.cases("pdk/cases", "input")) {
            String input = OracleCorpus.read("pdk/cases/" + name + ".input");
            if (java.nio.file.Files.exists(OracleCorpus.resolve("pdk/cases/" + name + ".error"))) {
                var error = assertThrows(IllegalArgumentException.class,
                        () -> PdkCodec.serialize(PdkCodec.deserialize(input)), name);
                assertEquals(OracleCorpus.read("pdk/cases/" + name + ".error"), error.getMessage(), name);
            } else {
                assertEquals(OracleCorpus.read("pdk/cases/" + name + ".expected"),
                        PdkCodec.serialize(PdkCodec.deserialize(input)), name);
            }
        }
    }
}
