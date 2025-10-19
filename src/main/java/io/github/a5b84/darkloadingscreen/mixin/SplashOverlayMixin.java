package io.github.a5b84.darkloadingscreen.mixin;

import static io.github.a5b84.darkloadingscreen.LoadingScreenConfig.ALPHA_MASK;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.a5b84.darkloadingscreen.AutoTheme;
import io.github.a5b84.darkloadingscreen.DarkLoadingScreen;
import io.github.a5b84.darkloadingscreen.DrawTextureLambda;
import io.github.a5b84.darkloadingscreen.LoadingScreenConfig;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Mutable
    @Shadow
    private static @Final IntSupplier BRAND_ARGB;

    @Unique
    private static final boolean IS_MOD_ENABLED = AutoTheme.IS_ENABLED && DarkLoadingScreen.CONFIG != null;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void adjustBg(CallbackInfo ci) {
        if (IS_MOD_ENABLED) {
            BRAND_ARGB = () -> DarkLoadingScreen.CONFIG.bg;
        }
    }

    @SuppressWarnings({"DataFlowIssue", "InvokeAssignCanReplacedWithExpression"})
    @ModifyVariable(
            method = "renderProgressBar",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/math/ColorHelper;getArgb(IIII)I"),
            ordinal = 6)
    private int modifyBarColor(int barColor, DrawContext context, int x1, int y1, int x2, int y2, float opacity) {
        if (!IS_MOD_ENABLED) {
            return barColor;
        }
        int alpha = barColor & ALPHA_MASK;
        LoadingScreenConfig config = DarkLoadingScreen.CONFIG;

        context.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, config.getBarBgColorWithAlpha(alpha >>> 24));
        return config.getBarColorWithAlpha(alpha >>> 24);
    }

    @ModifyVariable(
            method = "renderProgressBar",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                            ordinal = 0,
                            shift = At.Shift.AFTER),
            ordinal = 6)
    private int modifyBarBorderColor(int color) {
        if (!IS_MOD_ENABLED) {
            return color;
        }
        int alpha = color & ALPHA_MASK;
        //noinspection DataFlowIssue
        return DarkLoadingScreen.CONFIG.getBorderColorWithAlpha(alpha >>> 24);
    }

    @WrapOperation(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"))
    private void onDrawTexture(
            DrawContext context,
            RenderPipeline originalPipeline,
            Identifier sprite,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int regionWidth,
            int regionHeight,
            int textureWidth,
            int textureHeight,
            int color,
            Operation<Void> original) {
        if (!IS_MOD_ENABLED) {
            original.call(
                    context,
                    originalPipeline,
                    sprite,
                    x,
                    y,
                    u,
                    v,
                    width,
                    height,
                    regionWidth,
                    regionHeight,
                    textureWidth,
                    textureHeight,
                    color);
            return;
        }

        LoadingScreenConfig config = DarkLoadingScreen.CONFIG;
        int alpha = ColorHelper.getAlpha(color);

        DrawTextureLambda drawTexture = (pipeline, r, g, b) -> {
            if (r > 0 || g > 0 || b > 0) {
                original.call(
                        context,
                        pipeline,
                        sprite,
                        x,
                        y,
                        u,
                        v,
                        width,
                        height,
                        regionWidth,
                        regionHeight,
                        textureWidth,
                        textureHeight,
                        ColorHelper.getArgb(
                                alpha,
                                ColorHelper.channelFromFloat(Math.max(r, 0)),
                                ColorHelper.channelFromFloat(Math.max(g, 0)),
                                ColorHelper.channelFromFloat(Math.max(b, 0))));
            }
        };

        //noinspection DataFlowIssue
        drawTexture.call(
                DarkLoadingScreen.MOJANG_LOGO_SHADOWS, config.bgMinusLogoR, config.bgMinusLogoG, config.bgMinusLogoB);
        drawTexture.call(originalPipeline, config.logoMinusBgR, config.logoMinusBgG, config.logoMinusBgB);
    }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 500.0F))
    private float getFadeInTime(float old) {
        if (!IS_MOD_ENABLED) {
            return old;
        }
        //noinspection DataFlowIssue
        return DarkLoadingScreen.CONFIG.fadeInMs;
    }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 1000.0F))
    private float getFadeOutTime(float old) {
        if (!IS_MOD_ENABLED) {
            return old;
        }
        //noinspection DataFlowIssue
        return DarkLoadingScreen.CONFIG.fadeOutMs;
    }
}
