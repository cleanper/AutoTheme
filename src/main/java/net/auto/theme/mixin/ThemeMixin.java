package net.auto.theme.mixin;

import net.auto.theme.WindowOps;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
class ThemeMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onCreateWindow(CallbackInfo ci) {
        WindowOps.apply(MinecraftClient.getInstance().getWindow());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        WindowOps.apply(MinecraftClient.getInstance().getWindow());
    }
}
