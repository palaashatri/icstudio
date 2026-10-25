package dev.icstudio.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcStudioCliTest {
    @Test
    void optionValueIsRequiredAndNeverConsumesTheNextOption() {
        var args = new ArrayList<>(List.of("--output", "--project-root", "."));
        var error = assertThrows(IcStudioCli.CliUsageException.class, () -> IcStudioCli.takeOption(args, "--output"));
        assertTrue(error.getMessage().startsWith("--output requires a value"));
    }

    @Test
    void missingOptionIsEmptyAndLeavesArgumentsUntouched() {
        var args = new ArrayList<>(List.of("validate"));
        assertFalse(IcStudioCli.takeOption(args, "--name").isPresent());
        assertEquals(List.of("validate"), args);
    }

    @Test
    void unknownCommandsReportTheUsageText() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = IcStudioCli.run(new String[] {"frobnicate"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertEquals(1, status);
        String message = err.toString(StandardCharsets.UTF_8);
        assertTrue(message.startsWith("icstudio: unknown command 'frobnicate'\n\nUsage: icstudio"), message);
    }

    @Test
    void helpPrintsUsageAndSucceeds() {
        var out = new ByteArrayOutputStream();
        int status = IcStudioCli.run(new String[] {"help"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        assertEquals(0, status);
        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("Usage: icstudio [--project-root PATH]"));
    }
}
