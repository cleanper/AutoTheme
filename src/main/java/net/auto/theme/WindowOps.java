package net.auto.theme;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class WindowOps {
    private static boolean lastDark = false; // 上次应用的主题状态
    private static long lastHandle = 0; // 缓存的窗口句柄
    private static long lastThemeCheck = 0; // 上次主题检查时间
    private static final long THEME_CHECK_INTERVAL = 100; // 0.1秒检查一次主题变化

    private static final AtomicBoolean themeChangeDetected = new AtomicBoolean(false);
    private static final AtomicBoolean initializationComplete = new AtomicBoolean(false);

    private static IntByReference TRUE_REF;
    private static IntByReference FALSE_REF;
    private static WinDef.DWORD ATTRIBUTE;
    private static WinDef.DWORD SIZE;
    private static DwmApi DWMApiInstance;

    private static volatile boolean jniInitialized = false;

    private static void ensureInitialized() {
        if (initializationComplete.get()) return;

        synchronized (WindowOps.class) {
            if (initializationComplete.get()) return;

            TRUE_REF = new IntByReference(1);
            FALSE_REF = new IntByReference(0);
            ATTRIBUTE = new WinDef.DWORD(20L); // DWMWA_USE_IMMERSIVE_DARK_MODE
            SIZE = new WinDef.DWORD(4L);

            DWMApiInstance = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

            // 预初始化指针
            TRUE_REF.getPointer();
            FALSE_REF.getPointer();

            initializationComplete.set(true);
        }
    }

    /**
     * 强制应用当前主题到窗口
     * @param w Minecraft窗口实例
     */
    public static void apply(Window w) {
        ensureInitialized();
        if (jniInitialized && initializationComplete.get()) {
            applyTheme(w, true);
        }
    }

    /**
     * 仅在检测到主题变化或超过检查间隔时|应用主题
     */
    public static void applyIfNeeded(Window w) {
        if (!jniInitialized || !initializationComplete.get()) return;

        long currentTime = System.currentTimeMillis();
        boolean themeChanged = themeChangeDetected.getAndSet(false);

        if (themeChanged || currentTime - lastThemeCheck > THEME_CHECK_INTERVAL) {
            applyTheme(w, themeChanged);
            lastThemeCheck = currentTime;
        }
    }

    static void onThemeChanged() {
        themeChangeDetected.set(true);
        // 强制下次检查时重新应用主题
        if (jniInitialized) {
            lastDark = !AutoTheme.dark();
        }
    }

    public static void initializeJNI() {
        if (!jniInitialized) {
            AutoTheme.GetCurrentTheme();
            jniInitialized = true;
            lastDark = !AutoTheme.dark();
        }
    }

    private static long getWindowHandle(Window w) {
        if (lastHandle == 0) {
            lastHandle = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());
        }
        return lastHandle;
    }

    private static void applyTheme(Window w, boolean force) {
        if (!initializationComplete.get()) {
            ensureInitialized();
        }

        if (!initializationComplete.get() || DWMApiInstance == null) {
            return; // 如果初始化仍未完成，则跳过
        }

        boolean dark = AutoTheme.dark();

        // 如果主题未变化且不是强制应用则跳过
        if (!force && dark == lastDark) {
            return;
        }

        // 只有在主题实际变化或强制应用时才执行
        lastDark = dark;
        long handle = getWindowHandle(w);

        if (handle != 0) {
            IntByReference ref = dark ? TRUE_REF : FALSE_REF;

            DWMApiInstance.DwmSetWindowAttribute(
                    new WinDef.HWND(Pointer.createConstant(handle)),
                    ATTRIBUTE,
                    ref.getPointer(),
                    SIZE);
            // System.out.println("窗口主题已应用: " + (dark ? "深色模式" : "浅色模式"));
        }
    }

    private interface DwmApi extends StdCallLibrary {
        void DwmSetWindowAttribute(WinDef.HWND hwnd, WinDef.DWORD attr, Pointer data, WinDef.DWORD size);
    }
}
