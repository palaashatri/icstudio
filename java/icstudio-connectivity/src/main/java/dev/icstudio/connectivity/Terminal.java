package dev.icstudio.connectivity;

import java.util.Objects;

/** A device pin reference: an instance name and one of its terminal names. */
public record Terminal(String instance, String pin) {
    public Terminal {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(pin, "pin");
        if (instance.isEmpty() || pin.isEmpty()) {
            throw new IllegalArgumentException("terminal instance and pin must not be empty");
        }
    }
}
