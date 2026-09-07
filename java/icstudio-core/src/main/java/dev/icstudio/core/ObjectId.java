package dev.icstudio.core;

import java.util.Locale;

/** Stable unsigned 128-bit identity represented by two raw 64-bit words. */
public record ObjectId(long high, long low) implements Comparable<ObjectId> {
    public static ObjectId parseHex(String value) {
        if (value == null || value.length() != 32) {
            throw new IllegalArgumentException("ObjectId must contain exactly 32 hexadecimal characters");
        }
        try {
            var high = Long.parseUnsignedLong(value.substring(0, 16), 16);
            var low = Long.parseUnsignedLong(value.substring(16, 32), 16);
            return new ObjectId(high, low);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("ObjectId contains non-hexadecimal characters", error);
        }
    }

    public String toHex() {
        return String.format(Locale.ROOT, "%016x%016x", high, low);
    }

    @Override
    public int compareTo(ObjectId other) {
        var highComparison = Long.compareUnsigned(high, other.high);
        return highComparison != 0 ? highComparison : Long.compareUnsigned(low, other.low);
    }
}
