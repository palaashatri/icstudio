package dev.icstudio.pdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TechnologyTest {
    @Test
    void deterministicRoundTripPreservesLayerPurposes() {
        var technology = Technology.create("open_demo", 1000);
        technology.addLayer(new Layer("metal1", "drawing", 68, 20));
        technology.addLayer(new Layer("metal1", "pin", 68, 16));
        var first = PdkCodec.serialize(technology);

        assertEquals(first, PdkCodec.serialize(PdkCodec.deserialize(first)));
        assertEquals(16, PdkCodec.deserialize(first).layer("metal1", "pin").orElseThrow().gdsDatatype());
    }

    @Test
    void duplicateGdsMappingIsRejected() {
        var technology = Technology.create("demo", 1000);
        technology.addLayer(new Layer("metal1", "drawing", 68, 20));
        var error = assertThrows(IllegalArgumentException.class,
                () -> technology.addLayer(new Layer("duplicate", "drawing", 68, 20)));
        assertEquals("GDS mapping 68/20 is already assigned", error.getMessage());
    }

    @Test
    void unsafeModelPathsAreRejected() {
        var technology = Technology.create("demo", 1000);
        assertThrows(IllegalArgumentException.class,
                () -> technology.addModel(new ModelReference("bad", "spice", "../secret")));
    }

    @Test
    void gdsLayerRangeIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> new Layer("m", "p", 70000, 0));
    }
}
