package io.github.a5b84.darkloadingscreen;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

public class DarkLoadingScreen {
    public static final String MOD_ID = "dark-loading-screen";
    public static final float VANILLA_FADE_IN_DURATION = 500;
    public static final float VANILLA_FADE_OUT_DURATION = 1000;

    public static final RenderPipeline MOJANG_LOGO_SHADOWS;
    public static final LoadingScreenConfig CONFIG;

    static {
        if (AutoTheme.isEnabled()) {
            MOJANG_LOGO_SHADOWS = RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                            .withLocation(Identifier.of(MOD_ID, "pipeline/mojang_logo_shadows"))
                            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE))
                            .build()
            );
            CONFIG = new LoadingScreenConfig();
            System.out.println("[DarkLoadingScreen] Config initialized for DARK theme");
        } else {
            MOJANG_LOGO_SHADOWS = null;
            CONFIG = null;
            System.out.println("[DarkLoadingScreen] Config NOT initialized - mod disabled for light mode");
        }
    }
}
