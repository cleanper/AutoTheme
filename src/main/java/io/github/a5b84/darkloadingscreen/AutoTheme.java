package io.github.a5b84.darkloadingscreen;

public final class AutoTheme {
    private static final String ARCH = System.getProperty("os.arch").toLowerCase();
    private static final boolean IS_64_BIT = ARCH.contains("64");

    static {
        String dllName = IS_64_BIT ? "AutoTheme_x64" : "AutoTheme_x86";
        try {
            System.loadLibrary(dllName);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load AutoTheme library: " + e.getMessage());
        }
    }

    private static native boolean dark0();

    private static final boolean IS_DARK_MODE;
    public static final boolean IS_ENABLED;

    static {
        boolean darkMode = true;
        try {
            darkMode = dark0();
            System.out.println("[DarkLoadingScreen] AutoTheme detected: " + (darkMode ? "DARK" : "LIGHT"));
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[DarkLoadingScreen] AutoTheme fallback: DARK (DLL error)");
        }
        IS_DARK_MODE = darkMode;
        IS_ENABLED = IS_DARK_MODE;

        if (!IS_ENABLED) {
            System.out.println("[DarkLoadingScreen] Mod DISABLED - System is in light mode");
        } else {
            System.out.println("[DarkLoadingScreen] Mod ENABLED - System is in dark mode");
        }
    }
}
