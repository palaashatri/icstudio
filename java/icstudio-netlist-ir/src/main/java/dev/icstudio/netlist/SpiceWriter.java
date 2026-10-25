package dev.icstudio.netlist;

/** Canonical, deterministic SPICE text for a validated netlist. */
public final class SpiceWriter {
    private SpiceWriter() {
    }

    public static String canonicalText(Netlist netlist) {
        netlist.validate();
        var output = new StringBuilder("* ").append(netlist.title()).append('\n');
        for (var circuit : netlist.circuits().values()) {
            output.append(".subckt ").append(circuit.name());
            for (var port : circuit.ports()) {
                output.append(' ').append(port);
            }
            output.append('\n');
            if (!circuit.parameters().isEmpty()) {
                output.append(".param");
                for (var parameter : circuit.parameters().entrySet()) {
                    output.append(' ').append(parameter.getKey()).append('=').append(parameter.getValue());
                }
                output.append('\n');
            }
            for (var instance : circuit.instances().values()) {
                output.append(instance.name());
                for (var terminal : instance.terminals()) {
                    output.append(' ').append(terminal);
                }
                output.append(' ').append(instance.modelOrValue());
                for (var parameter : instance.parameters().entrySet()) {
                    output.append(' ').append(parameter.getKey()).append('=').append(parameter.getValue());
                }
                output.append(" ; kind=").append(instance.kind().canonicalName()).append('\n');
            }
            output.append(".ends ").append(circuit.name()).append('\n');
        }
        return output.toString();
    }
}
