package net.auto.theme.mixin;

import net.auto.theme.WindowOps;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
class ThemeMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCreateWindow(CallbackInfo ci) {
        WindowOps.apply(MinecraftClient.getInstance().getWindow());
    }

    @Unique
    private static int frameCounter = 0;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        frameCounter++;
        // 每0.1秒检测一次主题变化（6帧≈0.1秒）
        if (frameCounter >= 6) {
            WindowOps.applyIfNeeded(MinecraftClient.getInstance().getWindow());
            frameCounter = 0;
        }
    }
}
