package dev.icstudio.ui.theme;

import java.awt.Font;
import javax.swing.UIManager;

/** Typography roles for ICStudio controls and workspaces. */
public final class StudioTypography {
    private StudioTypography() {
    }

    public static Font body() {
        return baseFont().deriveFont(Font.PLAIN, 13f);
    }

    public static Font control() {
        return baseFont().deriveFont(Font.PLAIN, 13f);
    }

    public static Font sectionHeading() {
        return baseFont().deriveFont(Font.BOLD, 12f);
    }

    public static Font title() {
        return baseFont().deriveFont(Font.BOLD, 15f);
    }

    public static Font monospaced() {
        var font = UIManager.getFont("TextArea.font");
        if (font != null && Font.MONOSPACED.equalsIgnoreCase(font.getFamily())) {
            return font.deriveFont(Font.PLAIN, 12f);
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    private static Font baseFont() {
        var font = UIManager.getFont("Label.font");
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }
}
