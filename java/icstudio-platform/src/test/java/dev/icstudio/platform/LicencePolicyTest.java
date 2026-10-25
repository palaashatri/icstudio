package dev.icstudio.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.platform.evidence.LicencePolicy;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LicencePolicyTest {
    @TempDir
    Path temporary;

    @Test
    void allowlistedLicencesPass() throws Exception {
        Path bom = temporary.resolve("bom.json");
        Files.writeString(bom, "{\"components\":[{\"type\":\"library\",\"group\":\"com.formdev\",\"name\":\"flatlaf\","
                + "\"version\":\"3.7.2\",\"licenses\":[{\"license\":{\"id\":\"Apache-2.0\"}}]}]}");

        assertEquals(List.of("com.formdev:flatlaf@3.7.2 Apache-2.0"), LicencePolicy.check(bom));
    }

    @Test
    void undeclaredOrDisallowedLicencesFailTheGate() throws Exception {
        Path bom = temporary.resolve("bad.json");
        Files.writeString(bom, "{\"components\":[{\"type\":\"library\",\"group\":\"x\",\"name\":\"y\","
                + "\"version\":\"1\",\"licenses\":[{\"license\":{\"id\":\"GPL-3.0-only\"}}]},"
                + "{\"type\":\"library\",\"group\":\"x\",\"name\":\"z\",\"version\":\"1\"}]}");

        var error = assertThrows(IllegalStateException.class, () -> LicencePolicy.check(bom));
        assertEquals("licence policy violation: x:y@1 uses disallowed licence GPL-3.0-only; x:z@1 declares no licence",
                error.getMessage());
    }
}
