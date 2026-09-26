package dev.icstudio.ui.catalog;

import dev.icstudio.ui.components.StudioButton;
import dev.icstudio.ui.components.StudioDisclosureGroup;
import dev.icstudio.ui.components.StudioInspector;
import dev.icstudio.ui.components.StudioSidebar;
import dev.icstudio.ui.components.StudioStatusBar;
import dev.icstudio.ui.components.StudioToolbar;
import dev.icstudio.ui.theme.StudioTokens;
import dev.icstudio.ui.theme.StudioTypography;
import dev.icstudio.ui.workspace.StudioWorkspace;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Developer-facing component catalog used for visual QA without requiring an
 * engineering project or solver stack.
 */
public final class StudioComponentCatalog extends JPanel {
    public StudioComponentCatalog() {
        super(new BorderLayout());

        var workspace = new StudioWorkspace();
        var toolbar = new StudioToolbar();
        toolbar.addItem(new StudioButton("Run", StudioButton.Variant.PRIMARY));
        toolbar.addItem(new StudioButton("Stop", StudioButton.Variant.DESTRUCTIVE));
        toolbar.addItem(new StudioButton("More", StudioButton.Variant.GHOST));

        var sidebar = new StudioSidebar();
        sidebar.setContent(section("Library", "No project is open."));

        var inspector = new StudioInspector();
        inspector.setContent(new StudioDisclosureGroup(
                "Selection",
                section(null, "Select an object to inspect its properties.")));

        var canvas = section("Component Catalog", "UI foundation preview — no engineering state.");
        canvas.setBorder(BorderFactory.createEmptyBorder(
                StudioTokens.SPACE_5,
                StudioTokens.SPACE_5,
                StudioTokens.SPACE_5,
                StudioTokens.SPACE_5));

        var status = new StudioStatusBar();
        status.setMessage("Ready");

        workspace.setToolbar(toolbar);
        workspace.setSidebar(sidebar);
        workspace.setContent(canvas);
        workspace.setInspector(inspector);
        workspace.setStatusBar(status);
        add(workspace, BorderLayout.CENTER);
    }

    private static JPanel section(String title, String message) {
        var panel = new JPanel(new GridLayout(0, 1, 0, StudioTokens.SPACE_2));
        panel.setOpaque(false);
        if (title != null) {
            var heading = new JLabel(title);
            heading.setFont(StudioTypography.title());
            panel.add(heading);
        }
        var body = new JLabel(message);
        body.setFont(StudioTypography.body());
        panel.add(body);
        return panel;
    }
}
