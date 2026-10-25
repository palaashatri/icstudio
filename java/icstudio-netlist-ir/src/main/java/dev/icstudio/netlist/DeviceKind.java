package dev.icstudio.netlist;

/** Structural device classes recognised by the M1 SPICE scaffold. */
public enum DeviceKind {
    RESISTOR("resistor"),
    CAPACITOR("capacitor"),
    VOLTAGE_SOURCE("voltage_source"),
    MOSFET("mosfet"),
    SUBCIRCUIT("subcircuit");

    private final String canonicalName;

    DeviceKind(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String canonicalName() {
        return canonicalName;
    }
}
