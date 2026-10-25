package dev.icstudio.results;

import java.util.List;

/**
 * A sampled signal. Samples are stored by exact IEEE-754 bit pattern, so round trips preserve
 * signed zeros, subnormals, and every finite value.
 */
public record Signal(String name, String unit, List<Double> axis, List<Double> values) {
    public Signal {
        axis = List.copyOf(axis);
        values = List.copyOf(values);
    }
}
