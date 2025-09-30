package io.github.a5b84.darkloadingscreen;

import net.fabricmc.api.ClientModInitializer;

public class DarkLoadingScreenClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (AutoTheme.IS_ENABLED) {
            System.out.println("[DarkLoadingScreen] Client initialized - Using DARK theme");
        } else {
            System.out.println("[DarkLoadingScreen] Client initialized - Mod DISABLED (light mode)");
        }
    }
}
