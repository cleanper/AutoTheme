package net.auto.theme;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AutoTheme {
    private static boolean libraryLoaded = false;
    private static final SubmissionPublisher<Boolean> themePublisher = new SubmissionPublisher<>();

    private static final Thread.Builder virtualThreadBuilder = Thread.ofVirtual().name("AutoTheme-Virtual-", 0);

    private static final AtomicInteger currentSystemTheme = new AtomicInteger(0);

    private static volatile boolean appInitialized = false;

    private static final AtomicBoolean monitorStarted = new AtomicBoolean(false);
    private static final AtomicBoolean directCallbackInitialized = new AtomicBoolean(false);

    static {
        loadLibrary();
    }

    private static void loadLibrary() {
        if (!libraryLoaded) {
            String dllName = getPlatformDllName();
            System.loadLibrary(dllName);
            libraryLoaded = true;
        }
    }

    private static String getPlatformDllName() {
        String arch = System.getProperty("os.arch").toLowerCase();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            if (arch.contains("64") || arch.equals("amd64") || arch.equals("x86_64")) {
                return "AutoTheme_x64"; // 若系统为64位架构加载X64的库
            } else if (arch.contains("86")) {
                return "AutoTheme_x86"; // 若系统为32位架构加载X86的库
            }
        }
        throw new UnsupportedOperationException("Unsupported platform: " + os + "/" + arch); // 若为其他架构则抛出不兼容提示
    }

    public static native int GetCurrentTheme(); // 获取当前主题状态 (0=浅色, 1=深色)

    public static native void StartMonitor(); // 启动主题监控
    public static native void StopMonitor(); // 停止主题监控

    private static native void SetDirectThemeCallback();

    @SuppressWarnings("unused")
    public static Flow.Publisher<Boolean> themeChanges() {
        return themePublisher;
    }

    public static boolean dark() {
        return currentSystemTheme.get() == 1;
    }

    @SuppressWarnings("unused")
    private static void onSystemThemeChanged(int newTheme) {
        int oldTheme = currentSystemTheme.getAndSet(newTheme);

        if (oldTheme != newTheme) {
            // 发布主题变化事件
            virtualThreadBuilder.start(() -> {
                boolean isDark = (newTheme == 1);
                themePublisher.submit(isDark);
                WindowOps.onThemeChanged(isDark);
            });
            // System.out.println("AutoTheme: 收到系统主题变化通知，新主题: " + (newTheme == 1 ? "深色" : "浅色"));
        }
    }

    public static void startThemeMonitoring() {
        if (monitorStarted.compareAndSet(false, true)) {
            currentSystemTheme.set(GetCurrentTheme()); // 获取初始主题

            if (directCallbackInitialized.compareAndSet(false, true)) {
                SetDirectThemeCallback();
            }

            virtualThreadBuilder.start(() -> {
                try {
                    StartMonitor();
                } catch (Exception e) {
                    // 静默处理异常
                    monitorStarted.set(false);
                }
            });
            // System.out.println("AutoTheme: 主题监控已启动，初始主题: " + (currentSystemTheme.get() == 1 ? "深色" : "浅色"));
        }
    }

    public static void stopThemeMonitoring() {
        if (monitorStarted.compareAndSet(true, false)) {
            StopMonitor();
            // System.out.println("AutoTheme: 主题监控已停止");
        }
    }

    public static void initialize() {
        if (appInitialized) {
            return;
        }

        startThemeMonitoring();

        Thread shutdownThread = virtualThreadBuilder.unstarted(AutoTheme::stopThemeMonitoring);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        appInitialized = true;
    }
}
