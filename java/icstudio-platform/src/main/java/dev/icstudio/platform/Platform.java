package dev.icstudio.platform;

import java.util.Locale;

/** Host operating-system identity used to select platform-specific adapters. */
public enum Platform {
    MACOS,
    WINDOWS,
    LINUX,
    OTHER;

    public static Platform current() {
        return fromOsName(System.getProperty("os.name", ""));
    }

    static Platform fromOsName(String osName) {
        String normalized = osName.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("mac")) {
            return MACOS;
        }
        if (normalized.startsWith("windows")) {
            return WINDOWS;
        }
        if (normalized.contains("linux")) {
            return LINUX;
        }
        return OTHER;
    }

    /**
     * Whether a native backdrop-blur material has been implemented and verified for this platform.
     *
     * <p>No platform adapter is verified yet, so this is always {@code false}. Semantic materials
     * therefore resolve to their opaque fallbacks, which keeps the workbench usable everywhere.</p>
     */
    public boolean nativeBackdropBlurVerified() {
        return false;
    }
}
