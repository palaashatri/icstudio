package dev.icstudio.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.icstudio.ui.components.StudioInspector;
import dev.icstudio.ui.components.StudioSidebar;
import dev.icstudio.ui.components.StudioStatusBar;
import dev.icstudio.ui.components.StudioToolbar;
import dev.icstudio.ui.workspace.StudioWorkspace;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

final class StudioWorkspaceTest {
    @Test
    void slotsAreReplaceableWithoutLeakingOldComponents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var workspace = new StudioWorkspace();
            var toolbar = new StudioToolbar();
            var sidebar = new StudioSidebar();
            var firstContent = new JPanel();
            var secondContent = new JPanel();
            var inspector = new StudioInspector();
            var status = new StudioStatusBar();

            workspace.setToolbar(toolbar);
            workspace.setSidebar(sidebar);
            workspace.setContent(firstContent);
            workspace.setInspector(inspector);
            workspace.setStatusBar(status);

            assertEquals(5, workspace.getComponentCount());
            assertSame(firstContent, workspace.content());

            workspace.setContent(secondContent);

            assertEquals(5, workspace.getComponentCount());
            assertSame(secondContent, workspace.content());
            assertSame(workspace, secondContent.getParent());
        });
    }
}
