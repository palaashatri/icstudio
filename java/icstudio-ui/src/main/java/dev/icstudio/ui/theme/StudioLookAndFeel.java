package dev.icstudio.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;

/**
 * Installs the deterministic Swing plumbing used by ICStudio.
 *
 * <p>FlatLaf is infrastructure, not the product design language. Studio
 * components and semantic tokens remain the application-level API.</p>
 */
public final class StudioLookAndFeel {
    public enum Appearance {
        LIGHT,
        DARK
    }

    private StudioLookAndFeel() {
    }

    public static void install(Appearance appearance) {
        boolean installed = switch (appearance) {
            case LIGHT -> FlatLightLaf.setup();
            case DARK -> FlatDarkLaf.setup();
        };
        if (!installed) {
            throw new IllegalStateException("Unable to install ICStudio look and feel");
        }
        UIManager.put("Component.arc", StudioTokens.CORNER_RADIUS);
        UIManager.put("Button.arc", StudioTokens.CORNER_RADIUS);
        UIManager.put("Component.focusWidth", StudioTokens.FOCUS_WIDTH);
        UIManager.put("ScrollBar.width", 12);
    }
}
