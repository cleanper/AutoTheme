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
    private static volatile boolean lastDark = false; // 上次应用的主题状态
    private static long lastHandle = 0;

    private static final AtomicBoolean themeChangeDetected = new AtomicBoolean(true); // 初始为true以便首次应用
    private static final AtomicBoolean initializationComplete = new AtomicBoolean(false);

    private static IntByReference TRUE_REF;
    private static IntByReference FALSE_REF;
    private static WinDef.DWORD ATTRIBUTE;
    private static WinDef.DWORD SIZE;
    private static DwmApi DWMApiInstance;

    private static volatile boolean jniInitialized = false;

    static {
        new Thread(() -> Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS), "AutoTheme-Preload").start();
    }

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
            applyTheme(w);
        }
    }

    /**
     * 仅在检测到主题变化时|应用主题
     */
    public static void applyIfNeeded(Window w) {
        if (!jniInitialized || !initializationComplete.get()) return;

        if (themeChangeDetected.getAndSet(false)) {
            applyTheme(w);
        }
    }

    static void onThemeChanged() {
        themeChangeDetected.set(true);
        lastDark = !AutoTheme.dark(); // 强制下次|应用主题
    }

    public static void initializeJNI() {
        if (!jniInitialized) {
            AutoTheme.GetCurrentTheme();
            jniInitialized = true;
            lastDark = !AutoTheme.dark();
        }
    }

    private static long getWindowHandle(Window w) {
        long handle = lastHandle;
        if (handle == 0) {
            handle = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());
            lastHandle = handle;
        }
        return handle;
    }

    private static void applyTheme(Window w) {
        if (!initializationComplete.get()) {
            ensureInitialized();
            if (!initializationComplete.get()) return; // 如果初始化仍未完成则|跳过
        }

        boolean dark = AutoTheme.dark();

        // 如果主题未变化则|跳过
        if (dark == lastDark) {
            return;
        }

        // 主题发生变化时应用|新主题
        lastDark = dark;
        long handle = getWindowHandle(w);
        if (handle != 0) {
            applyThemeToWindow(handle, dark);
        }
    }

    private static void applyThemeToWindow(long handle, boolean dark) {
        IntByReference ref = dark ? TRUE_REF : FALSE_REF;
        DWMApiInstance.DwmSetWindowAttribute(
                new WinDef.HWND(Pointer.createConstant(handle)),
                ATTRIBUTE,
                ref.getPointer(),
                SIZE
        );
        // System.out.println("窗口主题已应用: " + (dark ? "深色模式" : "浅色模式"));
    }

    private interface DwmApi extends StdCallLibrary {
        void DwmSetWindowAttribute(WinDef.HWND hwnd, WinDef.DWORD attr, Pointer data, WinDef.DWORD size);
    }
}
