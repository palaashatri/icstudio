package dev.icstudio.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ICStudioArgumentsTest {
    @Test
    void projectOptionIsParsedWhenPresent() {
        assertEquals(Path.of("demo"), ICStudio.parseProject(new String[] {"--project", "demo"}));
        assertNull(ICStudio.parseProject(new String[0]));
    }

    @Test
    void projectOptionRequiresAValue() {
        var error = assertThrows(IllegalArgumentException.class, () -> ICStudio.parseProject(new String[] {"--project"}));
        assertEquals("--project requires a value", error.getMessage());
    }
}
