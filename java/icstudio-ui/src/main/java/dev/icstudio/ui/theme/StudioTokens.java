package dev.icstudio.ui.theme;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.UIManager;

/**
 * Semantic design tokens shared by ICStudio Swing components.
 *
 * <p>Screen code must depend on these names instead of introducing ad-hoc pixel
 * constants or reading look-and-feel keys directly.</p>
 */
public final class StudioTokens {
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int SPACE_5 = 24;
    public static final int SPACE_6 = 32;

    public static final int CONTROL_HEIGHT = 32;
    public static final int COMPACT_CONTROL_HEIGHT = 28;
    public static final int MIN_POINTER_TARGET = 28;
    public static final int SIDEBAR_WIDTH = 248;
    public static final int INSPECTOR_WIDTH = 288;
    public static final int TOOLBAR_HEIGHT = 40;
    public static final int STATUS_BAR_HEIGHT = 24;
    public static final int CORNER_RADIUS = 8;
    public static final int FOCUS_WIDTH = 2;

    private StudioTokens() {
    }

    public static Insets controlInsets() {
        return new Insets(SPACE_1, SPACE_3, SPACE_1, SPACE_3);
    }

    public static Insets panelInsets() {
        return new Insets(SPACE_3, SPACE_3, SPACE_3, SPACE_3);
    }

    public static Color windowBackground() {
        return uiColor("Panel.background", new Color(0xF5F5F7));
    }

    public static Color elevatedBackground() {
        return uiColor("Panel.background", new Color(0xFFFFFF));
    }

    public static Color sidebarBackground() {
        return uiColor("Tree.background", windowBackground());
    }

    public static Color inspectorBackground() {
        return uiColor("Panel.background", windowBackground());
    }

    public static Color toolbarBackground() {
        return uiColor("ToolBar.background", windowBackground());
    }

    public static Color canvasBackground() {
        return uiColor("TextArea.background", new Color(0xFAFAFA));
    }

    public static Color primaryText() {
        return uiColor("Label.foreground", new Color(0x1D1D1F));
    }

    public static Color secondaryText() {
        return uiColor("Label.disabledForeground", new Color(0x6E6E73));
    }

    public static Color accent() {
        return uiColor("Component.accentColor", new Color(0x0A84FF));
    }

    public static Color destructive() {
        return new Color(0xD70015);
    }

    public static Color separator() {
        return uiColor("Separator.foreground", new Color(0xD2D2D7));
    }

    private static Color uiColor(String key, Color fallback) {
        var color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}
