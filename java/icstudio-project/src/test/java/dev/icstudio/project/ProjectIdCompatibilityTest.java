package dev.icstudio.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.icstudio.core.ObjectId;
import org.junit.jupiter.api.Test;

final class ProjectIdCompatibilityTest {
    @Test
    void fnvDerivationMatchesTheRustM1Algorithm() {
        var parent = ObjectId.parseHex("0123456789abcdef0fedcba987654321");

        assertEquals(
                "cf8ed1fe362fe6d94cb385996b61e43a",
                ProjectIds.derive(parent, "library/analog").toHex());
    }
}
