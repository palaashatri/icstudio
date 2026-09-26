package dev.icstudio.core;

/** Unsigned 64-bit project revision with overflow-safe increment semantics. */
public record Revision(long value) implements Comparable<Revision> {
    public static final Revision ZERO = new Revision(0L);

    public static Revision parseUnsigned(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Revision must not be blank");
        }
        try {
            return new Revision(Long.parseUnsignedLong(value, 10));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Revision must be an unsigned 64-bit integer", error);
        }
    }

    public Revision next() {
        if (value == -1L) {
            throw new ArithmeticException("project revision overflow");
        }
        return new Revision(value + 1);
    }

    public String toUnsignedString() {
        return Long.toUnsignedString(value);
    }

    @Override
    public int compareTo(Revision other) {
        return Long.compareUnsigned(value, other.value);
    }
}
