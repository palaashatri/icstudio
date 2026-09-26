package dev.icstudio.project;

import dev.icstudio.core.ObjectId;
import java.nio.charset.StandardCharsets;

final class ProjectIds {
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long HIGH_OFFSET = 0xcbf29ce484222325L;
    private static final long LOW_OFFSET = 0x84222325cbf29ce4L;

    private ProjectIds() {
    }

    static ObjectId derive(ObjectId parent, String path) {
        return hash(parent.toHex() + ":" + path);
    }

    static ObjectId hash(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return new ObjectId(fnv1a64(bytes, HIGH_OFFSET), fnv1a64(bytes, LOW_OFFSET));
    }

    private static long fnv1a64(byte[] bytes, long offset) {
        long hash = offset;
        for (byte value : bytes) {
            hash ^= Byte.toUnsignedLong(value);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
