package dev.icstudio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.ui.theme.StudioTokens;
import dev.icstudio.ui.theme.StudioTypography;
import org.junit.jupiter.api.Test;

final class StudioTokensTest {
    @Test
    void spacingScaleIsMonotonicAndControlsMeetMinimumTarget() {
        assertTrue(StudioTokens.SPACE_1 < StudioTokens.SPACE_2);
        assertTrue(StudioTokens.SPACE_2 < StudioTokens.SPACE_3);
        assertTrue(StudioTokens.SPACE_3 < StudioTokens.SPACE_4);
        assertTrue(StudioTokens.SPACE_4 < StudioTokens.SPACE_5);
        assertTrue(StudioTokens.SPACE_5 < StudioTokens.SPACE_6);
        assertTrue(StudioTokens.CONTROL_HEIGHT >= StudioTokens.MIN_POINTER_TARGET);
    }

    @Test
    void semanticColorAndTypographyRolesAlwaysResolve() {
        assertNotNull(StudioTokens.windowBackground());
        assertNotNull(StudioTokens.sidebarBackground());
        assertNotNull(StudioTokens.inspectorBackground());
        assertNotNull(StudioTokens.canvasBackground());
        assertNotNull(StudioTokens.primaryText());
        assertNotNull(StudioTokens.secondaryText());
        assertNotNull(StudioTypography.body());
        assertNotNull(StudioTypography.control());
        assertNotNull(StudioTypography.sectionHeading());
    }

    @Test
    void mutableInsetsAreDefensiveValues() {
        var first = StudioTokens.controlInsets();
        var second = StudioTokens.controlInsets();

        assertNotSame(first, second);
        assertEquals(first, second);
    }
}
