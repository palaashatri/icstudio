package dev.icstudio.netlist;

import dev.icstudio.core.TextLines;
import dev.icstudio.core.Utf8Order;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A simulator-neutral structural netlist: a title, a top circuit, and subcircuit definitions. */
public final class Netlist {
    private final String title;
    private final String top;
    private final NavigableMap<String, Circuit> circuits = new TreeMap<>(Utf8Order.COMPARATOR);

    Netlist(String title, String top) {
        this.title = title;
        this.top = top;
    }

    public String title() {
        return title;
    }

    public String top() {
        return top;
    }

    public NavigableMap<String, Circuit> circuits() {
        return Collections.unmodifiableNavigableMap(circuits);
    }

    void putCircuit(Circuit circuit) {
        circuits.put(circuit.name(), circuit);
    }

    boolean containsCircuit(String name) {
        return circuits.containsKey(name);
    }

    public void validate() {
        if (TextLines.hasControl(title)) {
            throw new IllegalArgumentException("netlist title contains control characters");
        }
        if (!circuits.containsKey(top)) {
            throw new IllegalArgumentException("top circuit '" + top + "' does not exist");
        }
        for (var entry : circuits.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("circuit map key '" + entry.getKey() + "' is inconsistent");
            }
            entry.getValue().validate();
        }
    }
}
