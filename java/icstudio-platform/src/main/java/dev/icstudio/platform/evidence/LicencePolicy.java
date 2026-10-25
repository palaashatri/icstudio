package dev.icstudio.platform.evidence;

import dev.icstudio.platform.json.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dependency licence policy for the CycloneDX SBOM.
 *
 * <p>Every library component must declare at least one licence from the permissive allowlist.
 * Anything else, including an undeclared licence, fails the gate.</p>
 */
public final class LicencePolicy {
    public static final Set<String> ALLOWED_LICENCES = Set.of(
            "0BSD",
            "Apache-2.0",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "EPL-2.0",
            "ISC",
            "MIT");

    private LicencePolicy() {
    }

    /** Returns a sorted, human-readable summary of every library component and its licence. */
    public static List<String> check(Path cycloneDxBom) throws IOException {
        var document = asObject(Json.parse(Files.readString(cycloneDxBom, StandardCharsets.UTF_8)));
        var components = document.get("components");
        if (!(components instanceof List<?> items)) {
            throw new IllegalStateException("SBOM contains no components array");
        }
        var summary = new TreeSet<String>();
        var violations = new ArrayList<String>();
        for (Object item : items) {
            var component = asObject(item);
            if (!"library".equals(component.get("type"))) {
                continue;
            }
            String label = component.get("group") + ":" + component.get("name") + "@" + component.get("version");
            var licences = licences(component);
            if (licences.isEmpty()) {
                violations.add(label + " declares no licence");
                continue;
            }
            for (String licence : licences) {
                if (!ALLOWED_LICENCES.contains(licence)) {
                    violations.add(label + " uses disallowed licence " + licence);
                }
            }
            summary.add(label + " " + String.join(" OR ", licences));
        }
        if (summary.isEmpty()) {
            throw new IllegalStateException("SBOM contains no library components");
        }
        if (!violations.isEmpty()) {
            throw new IllegalStateException("licence policy violation: " + String.join("; ", violations));
        }
        return List.copyOf(summary);
    }

    private static List<String> licences(Map<String, Object> component) {
        var licences = new TreeSet<String>();
        if (component.get("licenses") instanceof List<?> entries) {
            for (Object entry : entries) {
                var wrapper = asObject(entry);
                if (wrapper.get("license") instanceof Map<?, ?> license) {
                    Object identifier = license.get("id");
                    Object name = license.get("name");
                    if (identifier instanceof String id) {
                        licences.add(id);
                    } else if (name instanceof String value) {
                        licences.add(value);
                    }
                } else if (wrapper.get("expression") instanceof String expression) {
                    for (String term : expression.split(" OR ")) {
                        licences.add(term.trim());
                    }
                }
            }
        }
        return List.copyOf(licences);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException("SBOM value is not a JSON object");
    }
}
