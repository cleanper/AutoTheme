package net.auto.theme;

public final class AutoTheme {
    private static boolean libraryLoaded = false;

    static {
        loadLibrary();
    }

    private static void loadLibrary() {
        if (!libraryLoaded) {
            String arch = System.getProperty("os.arch").toLowerCase();
            String dllName = arch.contains("64") ? "AutoTheme_x64" : "AutoTheme_x86";
            System.loadLibrary(dllName); // 根据架构加载对应的 DLL
            libraryLoaded = true;
        }
    }

    public static native int GetCurrentTheme(); // 获取当前主题状态 (0=浅色, 1=深色)
    public static native void SetThemeCallback(Runnable callback); // 设置主题变化回调
    public static native void StartMonitor(); // 启动主题监控
    public static native void StopMonitor(); // 停止主题监控

    private static final long CHECK_INTERVAL = 100; // 全局缓存检查间隔 0.1秒
    private static final long THREAD_CACHE_INTERVAL = 16; // 线程级缓存间隔

    private static final ThreadLocal<ThemeCache> threadLocalCache =
            ThreadLocal.withInitial(ThemeCache::new);

    private static long lastGlobalCheck = 0;
    private static boolean globalCachedResult = false;
    private static final Object GLOBAL_LOCK = new Object();
    private static volatile boolean monitorStarted = false;

    private static class ThemeCache {
        long lastThreadCheck = 0;
        boolean threadCachedResult = false;
    }

    public static boolean dark() {
        ThemeCache threadCache = threadLocalCache.get();
        long currentTime = System.currentTimeMillis();

        if (currentTime - threadCache.lastThreadCheck <= THREAD_CACHE_INTERVAL) {
            return threadCache.threadCachedResult;
        }

        long lastGlobal;
        boolean globalResult;
        synchronized (GLOBAL_LOCK) {
            lastGlobal = lastGlobalCheck;
            globalResult = globalCachedResult;
        }

        if (currentTime - lastGlobal <= CHECK_INTERVAL) {
            threadCache.threadCachedResult = globalResult;
            threadCache.lastThreadCheck = currentTime;
            return globalResult;
        }

        return updateThemeCache(currentTime, threadCache);
    }

    private static boolean updateThemeCache(long currentTime, ThemeCache threadCache) {
        // 调用DLL获取当前主题状态
        int result = GetCurrentTheme();
        boolean boolResult = (result == 1);

        synchronized (GLOBAL_LOCK) {
            globalCachedResult = boolResult;
            lastGlobalCheck = currentTime;
        }

        threadCache.threadCachedResult = boolResult;
        threadCache.lastThreadCheck = currentTime;

        return boolResult;
    }

    static void notifyThemeChanged() {
        synchronized (GLOBAL_LOCK) {
            lastGlobalCheck = 0;
        }
        threadLocalCache.remove();
        WindowOps.onThemeChanged();
    }

    public static void startThemeMonitoring() {
        if (!monitorStarted) {
            // System.out.println("AutoTheme: 收到系统主题变化通知");
            SetThemeCallback(AutoTheme::notifyThemeChanged);
            new Thread(() -> {
                try {
                    StartMonitor();
                } catch (Exception e) {
                    // 静默处理异常
                }
            }, "AutoTheme-Monitor").start();
            monitorStarted = true;
            // System.out.println("AutoTheme: 主题监控已启动");
        }
    }

    public static void stopThemeMonitoring() {
        if (monitorStarted) {
            StopMonitor();
            monitorStarted = false;
            // System.out.println("AutoTheme: 主题监控已停止");
        }
    }

    public static void initialize() {
        startThemeMonitoring();
        Runtime.getRuntime().addShutdownHook(new Thread(AutoTheme::stopThemeMonitoring));
    }
}
