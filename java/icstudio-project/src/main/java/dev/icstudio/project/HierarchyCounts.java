package dev.icstudio.project;

/** Summary counts for the library/cell/view hierarchy. */
public record HierarchyCounts(int libraries, int cells, int views) {
    public HierarchyCounts {
        if (libraries < 0 || cells < 0 || views < 0) {
            throw new IllegalArgumentException("hierarchy counts must be non-negative");
        }
    }
}
