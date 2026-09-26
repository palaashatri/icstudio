package dev.icstudio.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.ui.components.StudioDisclosureGroup;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

final class StudioDisclosureGroupTest {
    @Test
    void expansionStateControlsContentVisibility() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var content = new JLabel("content");
            var group = new StudioDisclosureGroup("Properties", content, false);

            assertFalse(group.isExpanded());
            assertFalse(content.getParent().isVisible());
            assertSame(content, group.content());

            group.setExpanded(true);
            assertTrue(group.isExpanded());
            assertTrue(content.getParent().isVisible());
        });
    }
}
