package dev.icstudio.results;

import dev.icstudio.core.TextLines;
import java.util.ArrayList;
import java.util.List;

/** Deterministic codec for result manifests and exact-bit sample vectors (format version 1). */
public final class ResultCodec {
    static final String MAGIC = "ICSTUDIO_RESULTS";
    static final int VERSION = 1;

    private ResultCodec() {
    }

    public static String serialize(ResultSet results) {
        results.validate();
        var manifest = results.manifest();
        var output = new StringBuilder();
        output.append(MAGIC).append('\t').append(VERSION).append('\n');
        output.append("run\t")
                .append(escape(manifest.runId())).append('\t')
                .append(escape(manifest.projectId())).append('\t')
                .append(Long.toUnsignedString(manifest.projectRevision())).append('\t')
                .append(escape(manifest.analysis())).append('\t')
                .append(escape(manifest.inputHash())).append('\t')
                .append(escape(manifest.solver())).append('\t')
                .append(escape(manifest.solverVersion())).append('\n');
        for (var signal : results.signals().values()) {
            output.append("signal\t")
                    .append(escape(signal.name())).append('\t')
                    .append(escape(signal.unit())).append('\t')
                    .append(signal.values().size()).append('\n');
            for (int index = 0; index < signal.values().size(); index++) {
                output.append("sample\t")
                        .append(bits(signal.axis().get(index))).append('\t')
                        .append(bits(signal.values().get(index))).append('\n');
            }
        }
        return output.toString();
    }

    public static ResultSet deserialize(String input) {
        List<String> lines = TextLines.lines(input);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("result database is empty");
        }
        if (!lines.getFirst().equals(MAGIC + "\t" + VERSION)) {
            throw new IllegalArgumentException("unsupported result header '" + lines.getFirst() + "'");
        }
        if (lines.size() < 2) {
            throw new IllegalArgumentException("result run record is missing");
        }
        String[] run = lines.get(1).split("\t", -1);
        if (run.length != 8 || !"run".equals(run[0])) {
            throw new IllegalArgumentException("invalid result run record");
        }
        var manifest = new RunManifest(
                unescape(run[1]),
                unescape(run[2]),
                parseRevision(run[3]),
                unescape(run[4]),
                unescape(run[5]),
                unescape(run[6]),
                unescape(run[7]));

        var signals = new ArrayList<Signal>();
        int cursor = 2;
        while (cursor < lines.size()) {
            String[] fields = lines.get(cursor).split("\t", -1);
            if (fields.length != 4 || !"signal".equals(fields[0])) {
                throw new IllegalArgumentException("invalid signal record at line " + (cursor + 1));
            }
            String name = unescape(fields[1]);
            String unit = unescape(fields[2]);
            int count = parseCount(fields[3]);
            cursor++;
            var axis = new ArrayList<Double>(Math.min(count, 4096));
            var values = new ArrayList<Double>(Math.min(count, 4096));
            for (int sample = 0; sample < count; sample++) {
                if (cursor >= lines.size()) {
                    throw new IllegalArgumentException("signal '" + name + "' is truncated");
                }
                String[] sampleFields = lines.get(cursor).split("\t", -1);
                if (sampleFields.length != 3 || !"sample".equals(sampleFields[0])) {
                    throw new IllegalArgumentException("invalid sample record at line " + (cursor + 1));
                }
                axis.add(Double.longBitsToDouble(parseBits(sampleFields[1])));
                values.add(Double.longBitsToDouble(parseBits(sampleFields[2])));
                cursor++;
            }
            signals.add(new Signal(name, unit, axis, values));
        }
        return new ResultSet(manifest, signals);
    }

    private static long parseRevision(String value) {
        try {
            return Long.parseUnsignedLong(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid project revision '" + value + "'", error);
        }
    }

    private static int parseCount(String value) {
        try {
            return Integer.parseUnsignedInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid signal sample count '" + value + "'", error);
        }
    }

    private static long parseBits(String value) {
        if (value.length() != 16 || !value.chars().allMatch(ResultCodec::isHexDigit)) {
            throw new IllegalArgumentException("invalid IEEE-754 bit pattern '" + value + "'");
        }
        return Long.parseUnsignedLong(value, 16);
    }

    private static boolean isHexDigit(int character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    private static String bits(double value) {
        return String.format("%016x", Double.doubleToRawLongBits(value));
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    static String unescape(String value) {
        var output = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\\') {
                output.append(character);
                continue;
            }
            if (++index >= value.length()) {
                throw new IllegalArgumentException("unterminated result field escape");
            }
            switch (value.charAt(index)) {
                case '\\' -> output.append('\\');
                case 't' -> output.append('\t');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                default -> throw new IllegalArgumentException(
                        "unsupported result field escape '\\" + value.charAt(index) + "'");
            }
        }
        return output.toString();
    }
}
