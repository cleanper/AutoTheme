package net.auto.theme.mixin;

import net.auto.theme.AutoTheme;
import net.auto.theme.WindowOps;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ThemeMixin {
    @Unique
    private static boolean globalThemeInitialized = false;
    @Unique
    private boolean firstRender = true;

    @Inject(method = "render", at = @At("HEAD"))
    private void onEveryFrame(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (firstRender && client.window != null) {
            if (!globalThemeInitialized) {
                WindowOps.initializeJNI();
                AutoTheme.initialize();
                globalThemeInitialized = true;
            }
            WindowOps.apply(client.window);
            firstRender = false;
        } else if (client.window != null) {
            WindowOps.applyIfNeeded(client.window);
        }
    }
}
