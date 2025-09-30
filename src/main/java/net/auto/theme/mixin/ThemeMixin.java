package net.auto.theme.mixin;

import net.auto.theme.WindowOps;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ThemeMixin {

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void onConstructorReturn(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.window != null) {
            WindowOps.apply(client.window);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onEveryFrame(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.window != null) {
            WindowOps.applyIfNeeded(client.window);
        }
    }
}
