package net.auto.theme;

final class AutoTheme {
    static {
        String arch = System.getProperty("os.arch");
        String dllName = arch.contains("64") ? "AutoTheme_x64" : "AutoTheme_x86";
        System.loadLibrary(dllName); // 根据架构加载对应的 DLL
    }
    private static native boolean dark0(); // JNI 入口

    private static long lastCheckTime = 0;
    private static boolean cachedResult = false;
    private static final long CHECK_INTERVAL = 100; // 0.1秒检测间隔

    // @SuppressWarnings("StatementWithEmptyBody")
    static boolean dark() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime > CHECK_INTERVAL) //noinspection CommentedOutCode
        {
            cachedResult = dark0();

            // 控制台调试
            // boolean newResult = dark0();
            // if (newResult != cachedResult) {
            //     System.out.println("AutoTheme.dark() = " + newResult);
            // }
            // cachedResult = newResult;

            lastCheckTime = currentTime;
        }
        return cachedResult;
    }
}
