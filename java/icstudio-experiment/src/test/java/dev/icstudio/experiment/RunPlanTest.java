package dev.icstudio.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RunPlanTest {
    @Test
    void planBindsToTheProjectRevisionAndRejectsControlCharacters() {
        var plan = new RunPlan("run-1", "project-1", 4L, "transient");

        assertEquals(4L, plan.projectRevision());
        assertThrows(IllegalArgumentException.class,
                () -> new RunPlan("bad\tid", "project-1", 4L, "transient"));
    }
}
