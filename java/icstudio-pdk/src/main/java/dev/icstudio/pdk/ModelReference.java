package dev.icstudio.pdk;

/** A named model reference; the path is relative and cannot escape the technology directory. */
public record ModelReference(String name, String kind, String relativePath) {
}
