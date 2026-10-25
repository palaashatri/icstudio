package dev.icstudio.pdk;

import dev.icstudio.core.TextLines;
import java.util.ArrayList;
import java.util.List;

/** Deterministic line-oriented codec for PDK metadata version 1. */
public final class PdkCodec {
    static final String MAGIC = "ICSTUDIO_PDK";

    private PdkCodec() {
    }

    public static String serialize(Technology technology) {
        technology.validate();
        var output = new StringBuilder();
        output.append(MAGIC).append('\t').append(Technology.SCHEMA_VERSION).append('\n');
        output.append("technology\t")
                .append(escapeField(technology.name())).append('\t')
                .append(technology.dbuPerMicron()).append('\n');
        for (var layer : technology.layers().values()) {
            output.append("layer\t")
                    .append(escapeField(layer.name())).append('\t')
                    .append(escapeField(layer.purpose())).append('\t')
                    .append(layer.gdsLayer()).append('\t')
                    .append(layer.gdsDatatype()).append('\n');
        }
        for (var model : technology.models().values()) {
            output.append("model\t")
                    .append(escapeField(model.name())).append('\t')
                    .append(escapeField(model.kind())).append('\t')
                    .append(escapeField(model.relativePath())).append('\n');
        }
        return output.toString();
    }

    public static Technology deserialize(String input) {
        List<String> lines = TextLines.lines(input);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("PDK metadata is empty");
        }
        String header = lines.getFirst();
        if (!header.equals(MAGIC + "\t" + Technology.SCHEMA_VERSION)) {
            throw new IllegalArgumentException("unsupported PDK header '" + header + "'");
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("technology record is missing");
        }
        String[] technologyFields = lines.get(1).split("\t", -1);
        if (technologyFields.length != 3 || !"technology".equals(technologyFields[0])) {
            throw new IllegalArgumentException("invalid technology record");
        }
        String name = unescapeField(technologyFields[1]);
        long dbu = parseUnsigned(technologyFields[2], "dbu_per_micron");
        var technology = Technology.create(name, dbu);

        for (int index = 2; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            switch (fields[0]) {
                case "layer" -> {
                    if (fields.length != 5) {
                        throw invalidRecord(index, line);
                    }
                    technology.addLayer(new Layer(
                            unescapeField(fields[1]),
                            unescapeField(fields[2]),
                            parseGds(fields[3], "GDS layer", index),
                            parseGds(fields[4], "GDS datatype", index)));
                }
                case "model" -> {
                    if (fields.length != 4) {
                        throw invalidRecord(index, line);
                    }
                    technology.addModel(new ModelReference(
                            unescapeField(fields[1]),
                            unescapeField(fields[2]),
                            unescapeField(fields[3])));
                }
                default -> throw invalidRecord(index, line);
            }
        }
        technology.validate();
        return technology;
    }

    private static IllegalArgumentException invalidRecord(int zeroBasedIndex, String line) {
        return new IllegalArgumentException(
                "invalid PDK record at line " + (zeroBasedIndex + 1) + ": '" + line + "'");
    }

    private static long parseUnsigned(String value, String kind) {
        try {
            long parsed = Long.parseUnsignedLong(value);
            if (parsed > 0xFFFF_FFFFL) {
                throw new NumberFormatException("value out of range");
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid " + kind + " '" + value + "': " + error.getMessage(), error);
        }
    }

    private static int parseGds(String value, String kind, int zeroBasedIndex) {
        try {
            int parsed = Integer.parseUnsignedInt(value);
            if (parsed > 0xFFFF) {
                throw new NumberFormatException("value out of range");
            }
            return parsed;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "invalid " + kind + " '" + value + "' at line " + (zeroBasedIndex + 1) + ": " + error.getMessage(),
                    error);
        }
    }

    static String escapeField(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> output.append("\\\\");
                case '\t' -> output.append("\\t");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                default -> output.append(character);
            }
        }
        return output.toString();
    }

    static String unescapeField(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                output.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IllegalArgumentException("unterminated field escape");
            }
            switch (value.charAt(index)) {
                case '\\' -> output.append('\\');
                case 't' -> output.append('\t');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                default -> throw new IllegalArgumentException(
                        "unsupported field escape '\\" + value.charAt(index) + "'");
            }
        }
        return output.toString();
    }
}
