package net.auto.theme.java21;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import javax.swing.JOptionPane;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

public class FJ21R implements ModInitializer {
    private static final Logger LOGGER = Logger.getLogger("ForceJava21 Reborn");

    @Override
    public void onInitialize() {
        Path configPath = FabricLoader.getInstance().getConfigDir();
        Path configFilePath = configPath.resolve("fj21r.properties");

        if (!Files.exists(configFilePath)) {
            genConfigFile(configFilePath);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFilePath)) {
            properties.load(inputStream);
        } catch (IOException ioe) {
            LOGGER.severe("Failed to load config file: " + ioe.getLocalizedMessage());
        }

        if (!isAllowedJavaVersionAndArch(properties)) {
            String crashInfo = genCrashInfo(properties);

            Object[] options = {"OK", "Copy and close"};
            int choice = JOptionPane.showOptionDialog(null, crashInfo, "WRONG JAVA VERSION OR ARCH!",
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options);

            if (choice == JOptionPane.NO_OPTION) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(crashInfo), null);
            }

            throw new RuntimeException("\n" + crashInfo);
        }

        LOGGER.info("Java version and arch check passed!");
    }

    private String genCrashInfo(Properties properties) {
        String needsJavaVersion = properties.getProperty("java_version", "");
        String needsJavaArch = properties.getProperty("java_arch", "");

        return String.format("Please use Java%s%s x%s to launch Minecraft!\n\nCurrent Java version: %s x%s\n\nRecommendations:\n- Amazon Corretto: %s\n- Azul Zulu: %s\n- Azul Platform Prime(Linux only): %s\n- Adoptium Eclipse Temurin: %s",
                isEmpty(needsJavaVersion) ? " [Any version]" : needsJavaVersion,
                Boolean.parseBoolean(properties.getProperty("enable_strict_check", "true")) ? "" : "(or later)",
                isEmpty(needsJavaArch) ? "[Any architecture]" : needsJavaArch,
                System.getProperty("java.specification.version"),
                System.getProperty("sun.arch.data.model"),
                properties.getProperty("amz_link", "https://aws.amazon.com/corretto/"),
                properties.getProperty("zulu_link", "https://www.azul.com/downloads/?package=jdk#zulu"),
                properties.getProperty("platform_prime_link", "https://www.azul.com/downloads/?package=jdk#prime"),
                properties.getProperty("temurin_link", "https://adoptium.net/temurin/releases/?package=jdk"));
    }

    private void genConfigFile(Path configFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath.toFile()))) {
            writer.write("java_version=\n");
            writer.write("java_arch=\n");
            writer.write("amz_link=https://aws.amazon.com/corretto/\n");
            writer.write("zulu_link=https://www.azul.com/downloads/?package=jdk#zulu\n");
            writer.write("platform_prime_link=https://www.azul.com/downloads/?package=jdk#prime\n");
            writer.write("temurin_link=https://adoptium.net/temurin/releases/?package=jdk\n");
            writer.write("enable_strict_check=true");

            LOGGER.info("Created config file at " + configFilePath);
        } catch (IOException ioe) {
            LOGGER.severe("Failed to create config file: " + ioe.getLocalizedMessage());
        }
    }

    private boolean isAllowedJavaVersionAndArch(Properties properties) {
        float javaVersion = Float.parseFloat(System.getProperty("java.specification.version"));
        int javaArch = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        String needsJavaVersion = properties.getProperty("java_version", "");
        String needsJavaArch = properties.getProperty("java_arch", "");
        boolean strictCheck = Boolean.parseBoolean(properties.getProperty("enable_strict_check", "true"));

        return isVersionValid(needsJavaVersion, javaVersion, strictCheck) &&
                isArchValid(needsJavaArch, javaArch, strictCheck);
    }

    private boolean isVersionValid(String needsJavaVersion, float javaVersion, boolean strictCheck) {
        if (isEmpty(needsJavaVersion)) return true;
        float neededVersion = Float.parseFloat(needsJavaVersion);
        return strictCheck ? javaVersion == neededVersion : javaVersion >= neededVersion;
    }

    private boolean isArchValid(String needsJavaArch, int javaArch, boolean strictCheck) {
        if (isEmpty(needsJavaArch)) return true;
        int neededArch = Integer.parseInt(needsJavaArch);
        return strictCheck ? javaArch == neededArch : javaArch >= neededArch;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
