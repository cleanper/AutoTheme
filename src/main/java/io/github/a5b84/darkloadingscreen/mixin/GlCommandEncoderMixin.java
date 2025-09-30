package io.github.a5b84.darkloadingscreen.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.a5b84.darkloadingscreen.AutoTheme;
import io.github.a5b84.darkloadingscreen.DarkLoadingScreen;
import net.minecraft.client.gl.GlCommandEncoder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {

    @Shadow @Nullable private RenderPipeline currentPipeline;

    @Inject(method = "setPipelineAndApplyState", at = @At(value = "HEAD"))
    public void onSetPipelineAndApplyState(RenderPipeline newPipeline, CallbackInfo ci) {
        if (!AutoTheme.IS_ENABLED || DarkLoadingScreen.MOJANG_LOGO_SHADOWS == null) {
            return;
        }

        if (currentPipeline != newPipeline) {
            if (newPipeline == DarkLoadingScreen.MOJANG_LOGO_SHADOWS) {
                GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
            } else if (currentPipeline == DarkLoadingScreen.MOJANG_LOGO_SHADOWS) {
                GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            }
        }
    }
}
