package net.auto.theme;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFWNativeWin32;

public final class WindowOps {
    private static boolean lastDark = !AutoTheme.dark(); // 强制第一次刷新

    public static void apply(Window w) {
        boolean dark = AutoTheme.dark();
        if (dark == lastDark) return;
        lastDark = dark;

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(w.getHandle());
        // 申请 4 字节 native 内存，值为 1 或 0
        IntByReference ref = new IntByReference(dark ? 1 : 0);
        DwmApi.INSTANCE.DwmSetWindowAttribute(
                new WinDef.HWND(new Pointer(hwnd)),
                new WinDef.DWORD(20L), // DWMWA_USE_IMMERSIVE_DARK_MODE
                ref.getPointer(),
                new WinDef.DWORD(4L));
    }

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);
        void DwmSetWindowAttribute(WinDef.HWND hwnd, WinDef.DWORD attr, Pointer data, WinDef.DWORD size);
    }
}
