package dev.icstudio.ui.components;

import dev.icstudio.ui.theme.StudioTokens;
import dev.icstudio.ui.theme.StudioTypography;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Low-noise status surface for selection, coordinates, jobs, and diagnostics summaries. */
public final class StudioStatusBar extends JPanel {
    private final JLabel message = new JLabel();

    public StudioStatusBar() {
        super(new BorderLayout());
        setName("studio-status-bar");
        getAccessibleContext().setAccessibleName("Status bar");
        setPreferredSize(new Dimension(1, StudioTokens.STATUS_BAR_HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, StudioTokens.SPACE_2, 0, StudioTokens.SPACE_2));
        message.setFont(StudioTypography.body());
        message.setForeground(StudioTokens.secondaryText());
        add(message, BorderLayout.CENTER);
    }

    public void setMessage(String value) {
        message.setText(value == null ? "" : value);
    }

    public String message() {
        return message.getText();
    }
}
