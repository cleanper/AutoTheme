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
import java.util.concurrent.locks.StampedLock;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class WindowOps {
    private static volatile boolean lastDark = false; // 上次应用的主题状态
    private static long lastHandle = 0;

    private static final AtomicBoolean themeChangeDetected = new AtomicBoolean(true); // 初始为true以便首次应用
    private static final StampedLock windowLock = new StampedLock();

    private static final IntByReference TRUE_REF = new IntByReference(1);
    private static final IntByReference FALSE_REF = new IntByReference(0);
    private static final WinDef.DWORD ATTRIBUTE = new WinDef.DWORD(20L); // DWMWA_USE_IMMERSIVE_DARK_MODE
    private static final WinDef.DWORD SIZE = new WinDef.DWORD(4L);

    private static final DwmApi DWMApiInstance =
            Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

    private static volatile boolean jniInitialized = false;

    static {
        // 预初始化指针
        TRUE_REF.getPointer();
        FALSE_REF.getPointer();

        new Thread(() -> {
            try {
                DWMApiInstance.toString();
            } catch (Exception e) {
                // 静默处理
            }
        }, "AutoTheme-Preload").start();
    }

    /**
     * 强制应用当前主题到窗口
     * @param w Minecraft窗口实例
     */
    public static void apply(Window w) {
        if (jniInitialized) {
            applyTheme(w);
        }
    }

    /**
     * 仅在检测到主题变化时|应用主题
     */
    public static void applyIfNeeded(Window w) {
        if (!jniInitialized) return;

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
            AutoTheme.GetCurrentTheme(); // 预热 JNI 调用
            jniInitialized = true;
            long stamp = windowLock.writeLock();
            try {
                lastDark = !AutoTheme.dark();
            } finally {
                windowLock.unlockWrite(stamp);
            }
        }
    }

    private static long getWindowHandle(Window w) {
        long stamp = windowLock.tryOptimisticRead();
        long handle = lastHandle;
        if (!windowLock.validate(stamp)) {
            stamp = windowLock.readLock();
            try {
                handle = lastHandle;
            } finally {
                windowLock.unlockRead(stamp);
            }
        }

        if (handle == 0) {
            handle = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());
            stamp = windowLock.writeLock();
            try {
                lastHandle = handle;
            } finally {
                windowLock.unlockWrite(stamp);
            }
        }
        return handle;
    }

    private static void applyTheme(Window w) {
        boolean dark = AutoTheme.dark();

        long stamp = windowLock.tryOptimisticRead();
        boolean currentLastDark = lastDark;
        if (windowLock.validate(stamp) && dark == currentLastDark) {
            return; // 主题未变化则跳过
        }

        stamp = windowLock.readLock();
        try {
            if (dark == lastDark) {
                return;
            }
        } finally {
            windowLock.unlockRead(stamp);
        }

        // 主题发生变化时应用|新主题
        stamp = windowLock.writeLock();
        try {
            if (dark == lastDark) {
                return;
            }
            lastDark = dark;
        } finally {
            windowLock.unlockWrite(stamp);
        }

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
        @SuppressWarnings("UnusedReturnValue")
        int DwmSetWindowAttribute(
                WinDef.HWND hwnd,
                WinDef.DWORD dwAttribute,
                Pointer pvAttribute,
                WinDef.DWORD cbAttribute
        );
    }
}
