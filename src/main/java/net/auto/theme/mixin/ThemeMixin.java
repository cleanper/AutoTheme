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
    private boolean themeApplied = false;

    @Unique
    private static boolean globalThemeInitialized = false;

    @Inject(
            method = "<init>",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/MinecraftClient;window:Lnet/minecraft/client/util/Window;",
                            shift = At.Shift.AFTER))
    private void onWindowFieldSet(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (!globalThemeInitialized) {
            WindowOps.initializeJNI();
            AutoTheme.initialize();
            globalThemeInitialized = true;
        }

        if (!themeApplied) {
            WindowOps.apply(client.getWindow());
            themeApplied = true;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onEveryFrame(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        WindowOps.applyIfNeeded(client.getWindow());
    }
}
