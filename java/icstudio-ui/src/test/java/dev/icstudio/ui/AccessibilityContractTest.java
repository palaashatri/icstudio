package dev.icstudio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.ui.components.StudioButton;
import dev.icstudio.ui.components.StudioDisclosureGroup;
import dev.icstudio.ui.components.StudioInspector;
import dev.icstudio.ui.components.StudioSidebar;
import dev.icstudio.ui.components.StudioStatusBar;
import dev.icstudio.ui.components.StudioToolbar;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

final class AccessibilityContractTest {
    @Test
    void primaryInteractiveAndLandmarkComponentsExposeAccessibleNames() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var button = new StudioButton("Run", StudioButton.Variant.PRIMARY);
            var disclosure = new StudioDisclosureGroup("Properties", new JLabel("Body"));
            var sidebar = new StudioSidebar();
            var inspector = new StudioInspector();
            var toolbar = new StudioToolbar();
            var status = new StudioStatusBar();

            assertEquals("Run", button.getAccessibleContext().getAccessibleName());
            assertTrue(button.isFocusable());
            assertEquals("Properties section", disclosure.getAccessibleContext().getAccessibleName());
            assertEquals("Sidebar", sidebar.getAccessibleContext().getAccessibleName());
            assertEquals("Inspector", inspector.getAccessibleContext().getAccessibleName());
            assertEquals("Toolbar", toolbar.getAccessibleContext().getAccessibleName());
            assertEquals("Status bar", status.getAccessibleContext().getAccessibleName());
        });
    }
}
