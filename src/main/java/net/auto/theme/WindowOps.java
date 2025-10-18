package net.auto.theme;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFWNativeWin32;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class WindowOps {
    private static final ThreadLocal<Long> lastHandle = ThreadLocal.withInitial(() -> 0L);

    private static volatile Boolean lastDark = null; // 初始为null|确保首次应用

    private static volatile boolean themeChangeDetected = true; // 初始为true|以便首次应用
    private static volatile boolean pendingThemeToApply = false;
    private static volatile boolean jniInitialized = false;

    private static final Object THEME_LOCK = new Object();

    private static final IntByReference TRUE_REF = new IntByReference(1);
    private static final IntByReference FALSE_REF = new IntByReference(0);
    private static final WinDef.DWORD ATTRIBUTE = new WinDef.DWORD(20L); // DWMWA_USE_IMMERSIVE_DARK_MODE
    private static final WinDef.DWORD SIZE = new WinDef.DWORD(4L);

    private static final Pointer TRUE_PTR;
    private static final Pointer FALSE_PTR;

    private static final DwmApi DWMApiInstance =
            Native.loadLibrary("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

    private static final Thread.Builder threadBuilder = Thread.ofPlatform().daemon(true).name("AutoTheme-WindowOps-", 0);

    static {
        // 预初始化指针
        TRUE_PTR = TRUE_REF.getPointer();
        FALSE_PTR = FALSE_REF.getPointer();

        // 预加载|DWM API
        threadBuilder.start(() -> {
            try {
                DWMApiInstance.toString();
                // logger.fine("DWM API 预加载完成");
            } catch (Exception e) {
                // 静默处理
            }
        });
    }

    /**
     * 强制应用当前主题到窗口
     * @param w Minecraft窗口实例
     */
    public static void apply(Window w) {
        if (isJniInitialized()) {
            applyTheme(w);
        }
    }

    public static void applyIfNeeded(Window w) {
        if (!isJniInitialized()) return;

        if (themeChangeDetected) {
            applyPendingTheme(w);
        }
    }

    private static void applyPendingTheme(Window w) {
        if (themeChangeDetected) {
            synchronized (THEME_LOCK) {
                if (themeChangeDetected) {
                    themeChangeDetected = false;
                    long handle = getWindowHandle(w);
                    if (handle != 0) {
                        boolean theme = pendingThemeToApply;
                        threadBuilder.start(() -> applyThemeToWindow(handle, theme));
                        // System.out.println("应用待处理主题: " + (theme ? "深色模式" : "浅色模式"));
                    }
                }
            }
        }
    }

    static void onThemeChanged(boolean newDark) {
        Boolean currentLastDark = lastDark;

        if (currentLastDark != null && newDark == currentLastDark) {
            themeChangeDetected = false;
            return;
        }

        // 变化时更新状态
        synchronized (THEME_LOCK) {
            lastDark = newDark;
            pendingThemeToApply = newDark;
            themeChangeDetected = true;
        }

        // System.out.println("主题变化检测: " + (newDark ? "深色" : "浅色") + ", 上次主题: " + (currentLastDark != null ? (currentLastDark ? "深色" : "浅色") : "null"));
    }

    public static void initializeJNI() {
        if (!jniInitialized) {
            synchronized (THEME_LOCK) {
                if (!jniInitialized) {
                    int currentTheme = AutoTheme.GetCurrentTheme();
                    boolean isDark = (currentTheme == 1);
                    lastDark = isDark;
                    pendingThemeToApply = isDark;
                    themeChangeDetected = true; // 强制首次应用
                    jniInitialized = true;
                    // System.out.println("JNI 初始化完成，初始主题: " + (isDark ? "深色" : "浅色"));
                }
            }
        }
    }

    private static long getWindowHandle(Window w) {
        Long handle = lastHandle.get();
        if (handle == 0) {
            handle = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());
            lastHandle.set(handle);
        }
        return handle;
    }

    private static void applyTheme(Window w) {
        boolean dark = AutoTheme.dark();
        Boolean currentLastDark = lastDark;

        if (currentLastDark == null || dark != currentLastDark) {
            synchronized (THEME_LOCK) {
                currentLastDark = lastDark;
                if (currentLastDark == null || dark != currentLastDark) {
                    lastDark = dark;
                    long handle = getWindowHandle(w);
                    if (handle != 0) {
                        threadBuilder.start(() -> applyThemeToWindow(handle, dark));
                        pendingThemeToApply = dark; // 同步更新待应用主题
                        // System.out.println("直接应用主题: " + (dark ? "深色模式" : "浅色模式"));
                    }
                }
            }
        } else {
            long handle = getWindowHandle(w);
            if (handle != 0) {
                threadBuilder.start(() -> applyThemeToWindow(handle, dark));
                // System.out.println("强制首次应用主题: " + (dark ? "深色模式" : "浅色模式"));
            }
        }
    }

    private static void applyThemeToWindow(long handle, boolean dark) {
        Pointer attrPtr = dark ? TRUE_PTR : FALSE_PTR;
        DWMApiInstance.DwmSetWindowAttribute(
                // int result = DWMApiInstance.DwmSetWindowAttribute(
                new WinDef.HWND(Pointer.createConstant(handle)),
                ATTRIBUTE,
                attrPtr,
                SIZE
        );
        // System.out.println("窗口主题应用: " + (dark ? "深色模式" : "浅色模式") + ", 结果代码: " + result + ", 窗口句柄: " + handle);
    }

    private static boolean isJniInitialized() {
        return jniInitialized;
    }

    private interface DwmApi extends StdCallLibrary {
        @SuppressWarnings("UnusedReturnValue")
        int DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                WinDef.DWORD dwAttribute,
                Pointer pvAttribute,
                WinDef.DWORD cbAttribute
        );
    }
}
