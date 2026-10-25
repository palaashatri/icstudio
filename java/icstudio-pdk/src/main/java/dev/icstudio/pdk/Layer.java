package dev.icstudio.pdk;

/** A layer-purpose pair bound to one GDSII layer and datatype. */
public record Layer(String name, String purpose, int gdsLayer, int gdsDatatype) {
    public Layer {
        if (gdsLayer < 0 || gdsLayer > 0xFFFF) {
            throw new IllegalArgumentException("GDS layer must be between 0 and 65535");
        }
        if (gdsDatatype < 0 || gdsDatatype > 0xFFFF) {
            throw new IllegalArgumentException("GDS datatype must be between 0 and 65535");
        }
    }
}
