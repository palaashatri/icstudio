package dev.icstudio.pdk;

import dev.icstudio.core.TextLines;
import dev.icstudio.core.Utf8Order;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Deterministic technology metadata: units, layer-purpose mappings, and model references.
 *
 * <p>This type does not execute PCells, rule decks, or compact models.</p>
 */
public final class Technology {
    public static final int SCHEMA_VERSION = 1;
    private static final long MAX_DBU_PER_MICRON = 0xFFFF_FFFFL;

    private final String name;
    private final long dbuPerMicron;
    private final NavigableMap<String, Layer> layers = new TreeMap<>(Utf8Order.COMPARATOR);
    private final NavigableMap<String, ModelReference> models = new TreeMap<>(Utf8Order.COMPARATOR);

    private Technology(String name, long dbuPerMicron) {
        this.name = name;
        this.dbuPerMicron = dbuPerMicron;
    }

    public static Technology create(String name, long dbuPerMicron) {
        validateIdentifier("technology", name);
        if (dbuPerMicron <= 0 || dbuPerMicron > MAX_DBU_PER_MICRON) {
            throw new IllegalArgumentException("dbu_per_micron must be greater than zero");
        }
        return new Technology(name, dbuPerMicron);
    }

    public String name() {
        return name;
    }

    public long dbuPerMicron() {
        return dbuPerMicron;
    }

    public NavigableMap<String, Layer> layers() {
        return Collections.unmodifiableNavigableMap(layers);
    }

    public NavigableMap<String, ModelReference> models() {
        return Collections.unmodifiableNavigableMap(models);
    }

    public void addLayer(Layer layer) {
        validateIdentifier("layer", layer.name());
        validateIdentifier("purpose", layer.purpose());
        var key = layerKey(layer.name(), layer.purpose());
        if (layers.containsKey(key)) {
            throw new IllegalArgumentException(
                    "layer-purpose '" + layer.name() + ":" + layer.purpose() + "' already exists");
        }
        for (var existing : layers.values()) {
            if (existing.gdsLayer() == layer.gdsLayer() && existing.gdsDatatype() == layer.gdsDatatype()) {
                throw new IllegalArgumentException(
                        "GDS mapping " + layer.gdsLayer() + "/" + layer.gdsDatatype() + " is already assigned");
            }
        }
        layers.put(key, layer);
    }

    public void addModel(ModelReference model) {
        validateIdentifier("model", model.name());
        validateIdentifier("model kind", model.kind());
        validateRelativePath(model.relativePath());
        if (models.containsKey(model.name())) {
            throw new IllegalArgumentException("model name already exists");
        }
        models.put(model.name(), model);
    }

    public Optional<Layer> layer(String layerName, String purpose) {
        return Optional.ofNullable(layers.get(layerKey(layerName, purpose)));
    }

    /** Re-validates every invariant, including map-key consistency and GDS uniqueness. */
    public void validate() {
        validateIdentifier("technology", name);
        var mappings = new java.util.HashSet<String>();
        for (var entry : layers.entrySet()) {
            var layer = entry.getValue();
            validateIdentifier("layer", layer.name());
            validateIdentifier("purpose", layer.purpose());
            if (!entry.getKey().equals(layerKey(layer.name(), layer.purpose()))) {
                throw new IllegalArgumentException("layer map key '" + entry.getKey() + "' is inconsistent");
            }
            if (!mappings.add(layer.gdsLayer() + "/" + layer.gdsDatatype())) {
                throw new IllegalArgumentException(
                        "duplicate GDS mapping " + layer.gdsLayer() + "/" + layer.gdsDatatype());
            }
        }
        for (var entry : models.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().name())) {
                throw new IllegalArgumentException("model map key '" + entry.getKey() + "' is inconsistent");
            }
            validateIdentifier("model", entry.getValue().name());
            validateIdentifier("model kind", entry.getValue().kind());
            validateRelativePath(entry.getValue().relativePath());
        }
    }

    static String layerKey(String layerName, String purpose) {
        return layerName + '\u001f' + purpose;
    }

    static void validateIdentifier(String kind, String value) {
        if (value == null || value.isEmpty()
                || TextLines.hasControl(value)
                || Utf8Order.utf8Length(value) > 128) {
            throw new IllegalArgumentException(kind + " must be 1 to 128 printable characters");
        }
    }

    static void validateRelativePath(String path) {
        if (path.isEmpty()
                || path.startsWith("/")
                || path.startsWith("\\")
                || java.util.Arrays.stream(path.split("[/\\\\]", -1)).anyMatch(".."::equals)
                || TextLines.hasControl(path)) {
            throw new IllegalArgumentException("model path '" + path + "' is not a safe relative path");
        }
    }
}
