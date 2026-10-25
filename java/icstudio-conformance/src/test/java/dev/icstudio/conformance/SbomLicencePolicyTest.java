package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.platform.evidence.LicencePolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Enforces the dependency licence policy against the CycloneDX SBOM produced by {@code verify}.
 *
 * <p>The SBOM must exist. A missing file fails the gate rather than skipping it, so the policy is never
 * silently unchecked.</p>
 */
class SbomLicencePolicyTest {
    @Test
    void everyLibraryInTheAggregateSbomUsesAnAllowlistedLicence() throws Exception {
        Path sbom = Path.of(System.getProperty("icstudio.sbom"));
        assertTrue(Files.isRegularFile(sbom), "run ./mvnw verify before the licence gate: missing " + sbom);

        List<String> summary = LicencePolicy.check(sbom);
        assertTrue(summary.stream().anyMatch(line -> line.startsWith("com.formdev:flatlaf@")), summary.toString());
        assertTrue(summary.stream().anyMatch(line -> line.contains("skija-shared")), summary.toString());
    }
}
