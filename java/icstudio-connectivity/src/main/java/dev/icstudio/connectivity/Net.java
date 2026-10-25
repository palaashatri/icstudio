package dev.icstudio.connectivity;

import java.util.List;
import java.util.Objects;

/**
 * A named electrical net and the terminals it joins.
 *
 * <p>This is a connectivity contract only. Electrical rule checks and netlisting are not yet
 * implemented, so no capability is claimed beyond holding and validating these values.</p>
 */
public record Net(String name, List<Terminal> terminals) {
    public Net {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("net name must not be empty");
        }
        terminals = List.copyOf(terminals);
    }
}
