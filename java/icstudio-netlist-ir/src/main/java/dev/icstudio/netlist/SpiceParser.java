package dev.icstudio.netlist;

import dev.icstudio.core.TextLines;
import dev.icstudio.core.Utf8Order;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded structural SPICE parser for the M1 scaffold.
 *
 * <p>Supports one level of {@code .subckt}/{@code .ends}, {@code .param}, and the R, C, V, M, and X
 * device designators. Values remain strings; no units, device equations, or model semantics.</p>
 */
public final class SpiceParser {
    private SpiceParser() {
    }

    public static Netlist parse(String input) {
        var title = new StringBuilder();
        var pending = new PendingNetlist();
        List<String> lines = TextLines.lines(input);
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = TextLines.trim(lines.get(index));
            if (line.isEmpty() || line.startsWith("*")) {
                continue;
            }
            if (title.isEmpty() && !line.startsWith(".")) {
                title.append(line);
                continue;
            }
            List<String> tokens = TextLines.splitWhitespace(line);
            if (tokens.isEmpty()) {
                continue;
            }
            String head = tokens.getFirst();
            if (TextLines.equalsIgnoreAsciiCase(head, ".subckt")) {
                pending.openCircuit(tokens, lineNumber);
            } else if (TextLines.equalsIgnoreAsciiCase(head, ".ends")) {
                pending.closeCircuit(lineNumber);
            } else if (TextLines.equalsIgnoreAsciiCase(head, ".param")) {
                pending.addParameters(tokens, lineNumber);
            } else if (line.startsWith(".")) {
                throw new IllegalArgumentException(
                        "unsupported SPICE directive '" + head + "' at line " + lineNumber);
            } else {
                pending.addInstance(parseInstance(tokens, lineNumber), lineNumber);
            }
        }
        pending.finish();
        var netlist = new Netlist(title.toString(), pending.top);
        for (var circuit : pending.circuits) {
            netlist.putCircuit(circuit);
        }
        netlist.validate();
        return netlist;
    }

    private static Instance parseInstance(List<String> tokens, int lineNumber) {
        String name = tokens.getFirst();
        NetlistNames.validate("instance", name);
        int first = name.codePointAt(0);
        int designator = first >= 'a' && first <= 'z' ? first - ('a' - 'A') : first;
        DeviceKind kind;
        int terminalCount;
        int modelIndex;
        switch (designator) {
            case 'R' -> {
                kind = DeviceKind.RESISTOR;
                terminalCount = 2;
                modelIndex = 3;
            }
            case 'C' -> {
                kind = DeviceKind.CAPACITOR;
                terminalCount = 2;
                modelIndex = 3;
            }
            case 'V' -> {
                kind = DeviceKind.VOLTAGE_SOURCE;
                terminalCount = 2;
                modelIndex = 3;
            }
            case 'M' -> {
                kind = DeviceKind.MOSFET;
                terminalCount = 4;
                modelIndex = 5;
            }
            case 'X' -> {
                if (tokens.size() < 4) {
                    throw new IllegalArgumentException("subcircuit instance is incomplete at line " + lineNumber);
                }
                kind = DeviceKind.SUBCIRCUIT;
                terminalCount = tokens.size() - 2;
                modelIndex = tokens.size() - 1;
            }
            default -> throw new IllegalArgumentException(
                    "unsupported SPICE device designator '" + Character.toString(designator) + "' at line " + lineNumber);
        }
        if (tokens.size() <= modelIndex || tokens.size() < terminalCount + 2) {
            throw new IllegalArgumentException("instance '" + name + "' is incomplete at line " + lineNumber);
        }
        var terminals = new ArrayList<>(tokens.subList(1, terminalCount + 1));
        var parameters = new java.util.TreeMap<String, String>(Utf8Order.COMPARATOR);
        for (String token : tokens.subList(modelIndex + 1, tokens.size())) {
            int equals = token.indexOf('=');
            if (equals < 0) {
                throw new IllegalArgumentException(
                        "invalid instance parameter '" + token + "' at line " + lineNumber);
            }
            String parameter = token.substring(0, equals);
            NetlistNames.validate("parameter", parameter);
            parameters.put(parameter, token.substring(equals + 1));
        }
        return new Instance(name, kind, terminals, tokens.get(modelIndex), parameters, lineNumber);
    }

    /** Mutable parse state that enforces the single-level subcircuit structure. */
    private static final class PendingNetlist {
        final List<Circuit> circuits = new ArrayList<>();
        final java.util.Set<String> names = new java.util.HashSet<>();
        Circuit active;
        String top;

        void openCircuit(List<String> tokens, int lineNumber) {
            if (active != null) {
                throw new IllegalArgumentException("nested .subckt at line " + lineNumber);
            }
            if (tokens.size() < 2) {
                throw new IllegalArgumentException(".subckt requires a name at line " + lineNumber);
            }
            String name = tokens.get(1);
            NetlistNames.validate("circuit", name);
            if (top == null) {
                top = name;
            }
            active = new Circuit(name, tokens.subList(2, tokens.size()));
        }

        void closeCircuit(int lineNumber) {
            if (active == null) {
                throw new IllegalArgumentException(".ends without .subckt at line " + lineNumber);
            }
            if (!names.add(active.name())) {
                throw new IllegalArgumentException("duplicate circuit '" + active.name() + "'");
            }
            circuits.add(active);
            active = null;
        }

        void addParameters(List<String> tokens, int lineNumber) {
            if (active == null) {
                throw new IllegalArgumentException(".param outside .subckt at line " + lineNumber);
            }
            for (String assignment : tokens.subList(1, tokens.size())) {
                int equals = assignment.indexOf('=');
                if (equals < 0) {
                    throw new IllegalArgumentException(
                            "invalid .param assignment '" + assignment + "' at line " + lineNumber);
                }
                String name = assignment.substring(0, equals);
                NetlistNames.validate("parameter", name);
                active.putParameter(name, assignment.substring(equals + 1));
            }
        }

        void addInstance(Instance instance, int lineNumber) {
            if (active == null) {
                throw new IllegalArgumentException("instance outside .subckt at line " + lineNumber);
            }
            if (!active.addInstance(instance)) {
                throw new IllegalArgumentException("duplicate instance at line " + lineNumber);
            }
        }

        void finish() {
            if (active != null) {
                throw new IllegalArgumentException(
                        "unterminated .subckt '" + active.name() + "': missing .ends");
            }
            if (top == null) {
                throw new IllegalArgumentException("SPICE input contains no .subckt");
            }
        }
    }

}
