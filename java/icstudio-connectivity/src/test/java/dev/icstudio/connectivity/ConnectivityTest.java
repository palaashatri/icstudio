package dev.icstudio.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConnectivityTest {
    @Test
    void netHoldsItsTerminalsImmutably() {
        var terminals = new java.util.ArrayList<>(List.of(new Terminal("M1", "d")));
        var net = new Net("out", terminals);
        terminals.add(new Terminal("M2", "d"));

        assertEquals(1, net.terminals().size());
    }

    @Test
    void emptyTerminalsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Terminal("", "d"));
        assertThrows(IllegalArgumentException.class, () -> new Net("", List.of()));
    }
}
