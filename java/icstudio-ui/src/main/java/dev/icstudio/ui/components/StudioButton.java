package dev.icstudio.ui.components;

import dev.icstudio.ui.theme.StudioTokens;
import dev.icstudio.ui.theme.StudioTypography;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JButton;

/** Project-owned button atom with semantic variants and stable sizing. */
public final class StudioButton extends JButton {
    public enum Variant {
        PRIMARY,
        SECONDARY,
        GHOST,
        DESTRUCTIVE
    }

    private Variant variant;

    public StudioButton(String text) {
        this(text, Variant.SECONDARY);
    }

    public StudioButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(StudioTypography.control());
        setMargin(StudioTokens.controlInsets());
        setFocusPainted(false);
        putClientProperty("JButton.buttonType", "roundRect");
        getAccessibleContext().setAccessibleName(text);
        updateVariant();
    }

    public Variant variant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("variant must not be null");
        }
        this.variant = variant;
        updateVariant();
    }

    @Override
    public Dimension getPreferredSize() {
        var preferred = super.getPreferredSize();
        return new Dimension(preferred.width, Math.max(preferred.height, StudioTokens.CONTROL_HEIGHT));
    }

    @Override
    public Insets getInsets() {
        var insets = super.getInsets();
        return new Insets(
                Math.max(insets.top, StudioTokens.SPACE_1),
                Math.max(insets.left, StudioTokens.SPACE_3),
                Math.max(insets.bottom, StudioTokens.SPACE_1),
                Math.max(insets.right, StudioTokens.SPACE_3));
    }

    private void updateVariant() {
        putClientProperty("icstudio.buttonVariant", variant.name());
        switch (variant) {
            case PRIMARY -> {
                setBackground(StudioTokens.accent());
                setForeground(java.awt.Color.WHITE);
                setContentAreaFilled(true);
            }
            case DESTRUCTIVE -> {
                setBackground(StudioTokens.destructive());
                setForeground(java.awt.Color.WHITE);
                setContentAreaFilled(true);
            }
            case SECONDARY -> {
                setBackground(StudioTokens.elevatedBackground());
                setForeground(StudioTokens.primaryText());
                setContentAreaFilled(true);
            }
            case GHOST -> {
                setForeground(StudioTokens.primaryText());
                setContentAreaFilled(false);
            }
        }
        repaint();
    }
}
