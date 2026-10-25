package dev.icstudio.netlist;

import java.util.List;
import java.util.NavigableMap;

/** One structural device or subcircuit instance; values are strings with no electrical semantics. */
public record Instance(
        String name,
        DeviceKind kind,
        List<String> terminals,
        String modelOrValue,
        NavigableMap<String, String> parameters,
        int sourceLine) {
    public Instance {
        terminals = List.copyOf(terminals);
        parameters = java.util.Collections.unmodifiableNavigableMap(new java.util.TreeMap<>(parameters));
    }
}
