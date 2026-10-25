package dev.icstudio.netlist;

import dev.icstudio.core.Utf8Order;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** A subcircuit definition with ports, nets, parameters, and instances in canonical order. */
public final class Circuit {
    private final String name;
    private final List<String> ports;
    private final NavigableSet<String> nets = new TreeSet<>(Utf8Order.COMPARATOR);
    private final NavigableMap<String, String> parameters = new TreeMap<>(Utf8Order.COMPARATOR);
    private final NavigableMap<String, Instance> instances = new TreeMap<>(Utf8Order.COMPARATOR);

    Circuit(String name, List<String> ports) {
        this.name = name;
        this.ports = new ArrayList<>(ports);
        this.nets.addAll(ports);
    }

    public String name() {
        return name;
    }

    public List<String> ports() {
        return Collections.unmodifiableList(ports);
    }

    public NavigableSet<String> nets() {
        return Collections.unmodifiableNavigableSet(nets);
    }

    public NavigableMap<String, String> parameters() {
        return Collections.unmodifiableNavigableMap(parameters);
    }

    public NavigableMap<String, Instance> instances() {
        return Collections.unmodifiableNavigableMap(instances);
    }

    void putParameter(String parameter, String value) {
        parameters.put(parameter, value);
    }

    /** Adds an instance, returning {@code false} when the name is already present. */
    boolean addInstance(Instance instance) {
        for (var net : instance.terminals()) {
            nets.add(net);
        }
        return instances.putIfAbsent(instance.name(), instance) == null;
    }

    void validate() {
        NetlistNames.validate("circuit", name);
        for (var port : ports) {
            NetlistNames.validate("port", port);
            if (!nets.contains(port)) {
                throw new IllegalArgumentException("port '" + port + "' is not present in circuit nets");
            }
        }
        for (var entry : instances.entrySet()) {
            var instance = entry.getValue();
            if (!entry.getKey().equals(instance.name())) {
                throw new IllegalArgumentException("instance map key '" + entry.getKey() + "' is inconsistent");
            }
            NetlistNames.validate("instance", instance.name());
            if (instance.terminals().isEmpty()) {
                throw new IllegalArgumentException("instance '" + instance.name() + "' has no terminals");
            }
            for (var net : instance.terminals()) {
                if (!nets.contains(net)) {
                    throw new IllegalArgumentException(
                            "instance '" + instance.name() + "' references missing net '" + net + "'");
                }
            }
        }
    }
}
