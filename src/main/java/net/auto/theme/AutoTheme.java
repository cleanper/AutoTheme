package net.auto.theme;

final class AutoTheme {
    static {
        System.loadLibrary("AutoTheme");   // 加载 AutoTheme.dll
    }
    private static native boolean dark0(); // JNI 入口

    static boolean dark() {
        return dark0();
    }
}
