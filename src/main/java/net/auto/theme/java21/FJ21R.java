package net.auto.theme.java21;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import javax.swing.JOptionPane;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

public class FJ21R implements ModInitializer {
    private static final Logger LOGGER = Logger.getLogger("ForceJava21 Reborn");
    private static final String JAVA_VERSION = System.getProperty("java.specification.version");
    private static final String JAVA_ARCH = System.getProperty("sun.arch.data.model");

    private static final float PARSED_JAVA_VERSION = Float.parseFloat(JAVA_VERSION);
    private static final int PARSED_JAVA_ARCH = Integer.parseInt(JAVA_ARCH);

    private static final String CONFIG_FILENAME = "fj21r.properties";
    private static final String DEFAULT_AMZ_LINK = "https://aws.amazon.com/corretto/";
    private static final String DEFAULT_ZULU_LINK = "https://www.azul.com/downloads/?package=jdk#zulu";
    private static final String DEFAULT_PRIME_LINK = "https://www.azul.com/downloads/?package=jdk#prime";
    private static final String DEFAULT_TEMURIN_LINK = "https://adoptium.net/temurin/releases/?package=jdk";

    @Override
    public void onInitialize() {
        Path configFilePath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);

        if (!Files.exists(configFilePath)) {
            genConfigFile(configFilePath);
        }

        Properties properties = loadProperties(configFilePath);
        if (properties == null) {
            return;
        }

        if (!isAllowedJavaVersionAndArch(properties)) {
            handleInvalidJavaEnvironment(properties);
        } else {
            LOGGER.info("Java version and arch check passed!");
        }
    }

    private Properties loadProperties(Path configFilePath) {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFilePath)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ioe) {
            LOGGER.severe("Failed to load config file: " + ioe.getLocalizedMessage());
            return null;
        }
    }

    private void handleInvalidJavaEnvironment(Properties properties) {
        String crashInfo = genCrashInfo(properties);

        Object[] options = {"OK", "Copy and close"};
        int choice = JOptionPane.showOptionDialog(null, crashInfo, "WRONG JAVA VERSION OR ARCH!",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options);

        if (choice == JOptionPane.NO_OPTION) {
            copyToClipboard(crashInfo);
        }

        throw new RuntimeException("\n" + crashInfo);
    }

    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Exception e) {
            LOGGER.warning("Failed to copy to clipboard: " + e.getMessage());
        }
    }

    private String genCrashInfo(Properties properties) {
        String needsJavaVersion = properties.getProperty("java_version", "");
        String needsJavaArch = properties.getProperty("java_arch", "");
        boolean strictCheck = Boolean.parseBoolean(properties.getProperty("enable_strict_check", "true"));

        String versionQualifier = isEmpty(needsJavaVersion) ? " [Any version]" : needsJavaVersion;
        String archQualifier = isEmpty(needsJavaArch) ? "[Any architecture]" : needsJavaArch;
        String strictQualifier = strictCheck ? "" : "(or later)";

        return String.format(
                "Please use Java%s%s x%s to launch Minecraft!%n%nCurrent Java version: %s x%s%n%nRecommendations:%n- Amazon Corretto: %s%n- Azul Zulu: %s%n- Azul Platform Prime(Linux only): %s%n- Adoptium Eclipse Temurin: %s",
                versionQualifier, strictQualifier, archQualifier,
                JAVA_VERSION, JAVA_ARCH,
                properties.getProperty("amz_link", DEFAULT_AMZ_LINK),
                properties.getProperty("zulu_link", DEFAULT_ZULU_LINK),
                properties.getProperty("platform_prime_link", DEFAULT_PRIME_LINK),
                properties.getProperty("temurin_link", DEFAULT_TEMURIN_LINK)
        );
    }

    private void genConfigFile(Path configFilePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(configFilePath)) {
            writer.write("java_version=\n");
            writer.write("java_arch=\n");
            writer.write("amz_link=" + DEFAULT_AMZ_LINK + "\n");
            writer.write("zulu_link=" + DEFAULT_ZULU_LINK + "\n");
            writer.write("platform_prime_link=" + DEFAULT_PRIME_LINK + "\n");
            writer.write("temurin_link=" + DEFAULT_TEMURIN_LINK + "\n");
            writer.write("enable_strict_check=true");

            LOGGER.info(() -> "Created config file at " + configFilePath); // 使用lambda延迟日志计算
        } catch (IOException ioe) {
            LOGGER.severe("Failed to create config file: " + ioe.getLocalizedMessage());
        }
    }

    private boolean isAllowedJavaVersionAndArch(Properties properties) {
        String needsJavaVersion = properties.getProperty("java_version", "");
        String needsJavaArch = properties.getProperty("java_arch", "");
        boolean strictCheck = Boolean.parseBoolean(properties.getProperty("enable_strict_check", "true"));

        return isVersionValid(needsJavaVersion, strictCheck) && isArchValid(needsJavaArch, strictCheck);
    }

    private boolean isVersionValid(String needsJavaVersion, boolean strictCheck) {
        if (isEmpty(needsJavaVersion)) return true;
        float neededVersion = Float.parseFloat(needsJavaVersion);
        return strictCheck ? PARSED_JAVA_VERSION == neededVersion : PARSED_JAVA_VERSION >= neededVersion;
    }

    private boolean isArchValid(String needsJavaArch, boolean strictCheck) {
        if (isEmpty(needsJavaArch)) return true;
        int neededArch = Integer.parseInt(needsJavaArch);
        return strictCheck ? PARSED_JAVA_ARCH == neededArch : PARSED_JAVA_ARCH >= neededArch;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
