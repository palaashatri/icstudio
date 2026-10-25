package dev.icstudio.netlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpiceParserTest {
    static final String INVERTER = "CMOS inverter\n.subckt inv in out vdd vss\n.param w=1u l=0.15u\n"
            + "M1 out in vdd vdd pmos w=2u l=0.15u\nM2 out in vss vss nmos w=1u l=0.15u\n.ends inv\n";

    @Test
    void parsesStructuralCmosInverter() {
        var netlist = SpiceParser.parse(INVERTER);
        var circuit = netlist.circuits().get("inv");

        assertEquals("inv", netlist.top());
        assertEquals(2, circuit.instances().size());
        assertEquals(List.of("in", "out", "vdd", "vss"), circuit.ports());
        assertEquals("1u", circuit.parameters().get("w"));
    }

    @Test
    void canonicalizationIsStable() {
        var first = SpiceWriter.canonicalText(SpiceParser.parse(INVERTER));
        assertEquals(first, SpiceWriter.canonicalText(SpiceParser.parse(INVERTER)));
        assertTrue(first.contains("M1 out in vdd vdd pmos"));
    }

    @Test
    void malformedInputReportsTheSourceLine() {
        var unsupported = assertThrows(IllegalArgumentException.class,
                () -> SpiceParser.parse("title\n.subckt bad a b\nQ1 a b c model\n.ends bad\n"));
        assertTrue(unsupported.getMessage().endsWith("at line 3"));
        var incomplete = assertThrows(IllegalArgumentException.class,
                () -> SpiceParser.parse("title\n.subckt bad a b\nR1 a\n.ends bad\n"));
        assertTrue(incomplete.getMessage().endsWith("at line 3"));
    }
}
