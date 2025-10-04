package net.auto.theme;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicStampedReference;

public final class AutoTheme {
    private static boolean libraryLoaded = false;
    private static final SubmissionPublisher<Boolean> themePublisher = new SubmissionPublisher<>();

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
    public static native void SetThemeCallback(Runnable callback); // 设置主题变化回调
    public static native void StartMonitor(); // 启动主题监控
    public static native void StopMonitor(); // 停止主题监控

    @SuppressWarnings("unused")
    public static Flow.Publisher<Boolean> themeChanges() {
        return themePublisher;
    }

    private static final AtomicStampedReference<ThemeState> currentThemeState =
            new AtomicStampedReference<>(new ThemeState(false, 0), 0);
    private static final long CACHE_TIMEOUT = 100; // 缓存超时时间(0.1秒)

    private record ThemeState(boolean isDark, long timestamp) {}

    public static boolean dark() {
        int[] stampHolder = new int[1];
        ThemeState state = currentThemeState.get(stampHolder);
        long currentTime = System.currentTimeMillis();

        if (currentTime - state.timestamp() <= CACHE_TIMEOUT) {
            return state.isDark();
        }

        return updateThemeCache();
    }

    private static boolean updateThemeCache() {
        long currentTime = System.currentTimeMillis();
        int result = GetCurrentTheme();
        boolean boolResult = (result == 1);
        ThemeState newState = new ThemeState(boolResult, currentTime);

        int[] oldStamp = new int[1];
        ThemeState oldState;
        do {
            oldState = currentThemeState.get(oldStamp);
            if (oldState.isDark() == boolResult &&
                    (currentTime - oldState.timestamp()) <= CACHE_TIMEOUT) {
                return boolResult;
            }
        } while (!currentThemeState.compareAndSet(oldState, newState, oldStamp[0], oldStamp[0] + 1));

        return boolResult;
    }

    static void notifyThemeChanged() {
        int[] stampHolder = new int[1];
        ThemeState current = currentThemeState.get(stampHolder);
        ThemeState newState = new ThemeState(false, 0);
        currentThemeState.compareAndSet(current, newState, stampHolder[0], stampHolder[0] + 1);

        // 发布主题变化事件
        boolean isDark = dark();
        themePublisher.submit(isDark);

        WindowOps.onThemeChanged();
    }

    private static volatile boolean monitorStarted = false;

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
