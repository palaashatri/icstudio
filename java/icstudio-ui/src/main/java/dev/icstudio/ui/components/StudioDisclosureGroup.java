package dev.icstudio.ui.components;

import dev.icstudio.ui.theme.StudioTokens;
import dev.icstudio.ui.theme.StudioTypography;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Keyboard-accessible disclosure section used in inspectors and sidebars. */
public final class StudioDisclosureGroup extends JPanel {
    private final StudioButton toggle;
    private final JPanel contentHost = new JPanel(new BorderLayout());
    private final String title;
    private boolean expanded;

    public StudioDisclosureGroup(String title, JComponent content) {
        this(title, content, true);
    }

    public StudioDisclosureGroup(String title, JComponent content, boolean expanded) {
        super(new BorderLayout(0, StudioTokens.SPACE_1));
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title;
        this.expanded = expanded;

        setOpaque(false);
        toggle = new StudioButton("", StudioButton.Variant.GHOST);
        toggle.setHorizontalAlignment(StudioButton.LEFT);
        toggle.setFont(StudioTypography.sectionHeading());
        toggle.addActionListener(event -> setExpanded(!isExpanded()));
        add(toggle, BorderLayout.NORTH);

        contentHost.setOpaque(false);
        if (content != null) {
            contentHost.add(content, BorderLayout.CENTER);
        }
        add(contentHost, BorderLayout.CENTER);

        getAccessibleContext().setAccessibleName(title + " section");
        refresh();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        refresh();
    }

    public JComponent content() {
        return contentHost.getComponentCount() == 0
                ? null
                : (JComponent) contentHost.getComponent(0);
    }

    private void refresh() {
        toggle.setText((expanded ? "▾ " : "▸ ") + title);
        toggle.getAccessibleContext().setAccessibleName((expanded ? "Collapse " : "Expand ") + title);
        contentHost.setVisible(expanded);
        revalidate();
        repaint();
    }
}
