package net.auto.theme;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFWNativeWin32;

@SuppressWarnings("ResultOfMethodCallIgnored") // 忽略getPointer结果
public final class WindowOps {
    private static boolean lastDark = !AutoTheme.dark(); // 强制第一次刷新
    private static long lastHandle = 0;

    private static final IntByReference TRUE_REF = new IntByReference(1);
    private static final IntByReference FALSE_REF = new IntByReference(0);
    private static final int ATTRIBUTE = 20; // DWMWA_USE_IMMERSIVE_DARK_MODE
    private static final int SIZE = 4;

    private static final int S_OK = 0x00000000;

    static {
        TRUE_REF.getPointer();
        FALSE_REF.getPointer();
    }

    public static void apply(Window w) {
        applyTheme(w, true);
    }

    // 只在主题变化时应用
    public static void applyIfNeeded(Window w) {
        applyTheme(w, false);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static void applyTheme(Window w, boolean force) {
        boolean dark = AutoTheme.dark();

        if (!force && dark == lastDark) {
            return;
        }
        lastDark = dark;

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());

        if (hwnd != lastHandle) {
            lastHandle = hwnd;
        }

        IntByReference ref = dark ? TRUE_REF : FALSE_REF;
        int result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                new Pointer(hwnd),
                ATTRIBUTE,
                ref.getPointer(),
                SIZE);

        if (result != S_OK) {
            // System.err.println("设置窗口主题失败，错误代码: 0x" + Integer.toHexString(result));
        } else {
            // System.out.println("窗口主题已应用: " + (dark ? "深色模式" : "浅色模式"));
        }
    }

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.loadLibrary("dwmapi", DwmApi.class);

        int DwmSetWindowAttribute(Pointer hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }
}
