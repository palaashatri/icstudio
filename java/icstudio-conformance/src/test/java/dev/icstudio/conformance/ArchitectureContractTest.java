package dev.icstudio.conformance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Repository-policy contract for ADR-0001: Java 25 and Swing are authoritative, and the retired stack is gone. */
class ArchitectureContractTest {
    private static final Path REPOSITORY = Path.of("../..").toAbsolutePath().normalize();

    @Test
    void constitutionRequiresTheJava25SwingStack() throws IOException {
        assertTrue(Files.isRegularFile(REPOSITORY.resolve("AGENTS.md")), "AGENTS.md must exist at the repository root");
        String constitution = Files.readString(REPOSITORY.resolve("docs/CONSTITUTION.md"));

        assertTrue(constitution.contains("Java 25"), "the constitution must name Java 25 as authoritative");
        assertTrue(constitution.contains("Swing"), "the constitution must name Swing as the desktop toolkit");
        assertTrue(constitution.contains("Skija") || constitution.contains("Skia"),
                "the constitution must define Skia-backed engineering canvases");
        assertTrue(constitution.contains("Foreign Function and Memory API") || constitution.contains("FFM"),
                "the constitution must define Java FFM as the preferred native boundary");
        assertFalse(constitution.contains("Electron + React/TypeScript shell"),
                "the Electron/React authoritative architecture must be removed");
        assertFalse(constitution.contains("Rust Platform Services"),
                "Rust must not remain the authoritative platform after ADR-0001");
    }

    @Test
    void agentGuideNamesJavaAsTheOnlyAuthoritativeStack() throws IOException {
        String agents = Files.readString(REPOSITORY.resolve("AGENTS.md"));

        assertTrue(agents.contains("Java 25") && agents.contains("Swing"), "AGENTS.md must name Java 25 and Swing");
        assertFalse(agents.contains("Electron/React workbench") && agents.contains("conformance oracle only"),
                "AGENTS.md must not describe the retired Electron workbench as a live surface");
    }

    @Test
    void retiredRustAndElectronApplicationStackIsAbsent() {
        assertFalse(Files.exists(REPOSITORY.resolve("crates")), "Rust crates must be removed after J1 parity");
        assertFalse(Files.exists(REPOSITORY.resolve("apps/workbench")), "the Electron workbench must be removed");
        assertFalse(Files.exists(REPOSITORY.resolve("Cargo.toml")), "the Rust workspace must be removed");
    }
}
