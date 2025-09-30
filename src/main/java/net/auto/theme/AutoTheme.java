package net.auto.theme;

final class AutoTheme {
    static {
        System.loadLibrary("AutoTheme");   // 加载 AutoTheme.dll
    }
    private static native boolean dark0(); // JNI 入口

    private static long lastCheckTime = 0;
    private static boolean cachedResult = false;
    private static final long CHECK_INTERVAL = 100; // 0.1秒检测间隔

    @SuppressWarnings("StatementWithEmptyBody")
    static boolean dark() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime > CHECK_INTERVAL) {
            boolean newResult = dark0();
            if (newResult != cachedResult) {
                // System.out.println("AutoTheme.dark() = " + newResult); // 控制台验证
            }
            cachedResult = newResult;
            lastCheckTime = currentTime;
        }
        return cachedResult;
    }
}
