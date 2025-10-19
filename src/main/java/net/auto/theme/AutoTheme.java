package net.auto.theme;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.jetbrains.annotations.NotNull;

public final class AutoTheme {
    private static boolean libraryLoaded = false;
    private static final SubmissionPublisher<Boolean> themePublisher = new SubmissionPublisher<>();

    private static volatile int currentSystemTheme = 0;

    private static volatile boolean appInitialized = false;

    private static volatile boolean monitorStarted = false;
    private static volatile boolean directCallbackInitialized = false;

    private static final Object INIT_LOCK = new Object();

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
            if (arch.contains("64") || "amd64".equals(arch) || "x86_64".equals(arch)) {
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
        return currentSystemTheme == 1;
    }

    @SuppressWarnings({"unused", "CommentedOutCode"})
    private static void onSystemThemeChanged(int newTheme) {
        int oldTheme = currentSystemTheme;

        // 如果没有变化则直接返回
        if (oldTheme == newTheme) {
            return;
        }

        currentSystemTheme = newTheme;

        boolean isDark = (newTheme == 1);
        themePublisher.submit(isDark);
        WindowOps.onThemeChanged(isDark);

        // System.out.println("AutoTheme: 收到系统主题变化通知，新主题: " + (newTheme == 1 ? "深色" : "浅色") + "，旧主题: " + (oldTheme == 1
        // ? "深色" : "浅色"));
    }

    public static void startThemeMonitoring() {
        if (monitorStarted) {
            return;
        }

        synchronized (INIT_LOCK) {
            if (!monitorStarted) {
                currentSystemTheme = GetCurrentTheme(); // 获取初始主题

                if (!directCallbackInitialized) {
                    SetDirectThemeCallback();
                    directCallbackInitialized = true;
                }

                Thread monitorThread = getMonitorThread();
                monitorThread.start();

                monitorStarted = true;
                // System.out.println("AutoTheme: 主题监控已启动，初始主题: " + (currentSystemTheme == 1 ? "深色" : "浅色"));
            }
        }
    }

    private static @NotNull Thread getMonitorThread() {
        Thread monitorThread = new Thread(
                () -> {
                    try {
                        StartMonitor();
                    } catch (Exception e) {
                        // 静默处理异常
                        synchronized (INIT_LOCK) {
                            monitorStarted = false;
                        }
                    }
                },
                "AutoTheme-Monitor");
        monitorThread.setDaemon(true);
        return monitorThread;
    }

    public static void stopThemeMonitoring() {
        if (monitorStarted) {
            synchronized (INIT_LOCK) {
                if (monitorStarted) {
                    StopMonitor();
                    monitorStarted = false;
                    // System.out.println("AutoTheme: 主题监控已停止");
                }
            }
        }
    }

    public static void initialize() {
        if (appInitialized) {
            return;
        }

        startThemeMonitoring();

        Thread shutdownThread = new Thread(AutoTheme::stopThemeMonitoring, "AutoTheme-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        appInitialized = true;
    }
}
