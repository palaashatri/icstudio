package dev.icstudio.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.icstudio.core.ObjectId;
import org.junit.jupiter.api.Test;

final class ProjectIdCompatibilityTest {
    @Test
    void fnvDerivationMatchesTheRustM1Algorithm() {
        var parent = ObjectId.parseHex("0123456789abcdef0fedcba987654321");

        assertEquals(
                "959882127a6b2fa87a91eb6c3d647857",
                ProjectIds.derive(parent, "library/analog").toHex());
    }
}
