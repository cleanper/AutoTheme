package io.github.a5b84.darkloadingscreen;

public class LoadingScreenConfig {
    public final int bg, bar, barBg, border, logo;
    public final float bgR, bgG, bgB;
    public final float logoR, logoG, logoB;
    public final float fadeInMs, fadeOutMs;

    public final float bgMinusLogoR, bgMinusLogoG, bgMinusLogoB;
    public final float logoMinusBgR, logoMinusBgG, logoMinusBgB;

    public final int barWithAlpha, barBgWithAlpha, borderWithAlpha;
    public static final int ALPHA_MASK = 0xff000000;

    public LoadingScreenConfig() {
        bg = 0x14181c;
        bar = 0xe22837;
        barBg = 0x14181c;
        border = 0x303336;
        logo = 0xffffff;

        bgR = getChannel(bg, 16);
        bgG = getChannel(bg, 8);
        bgB = getChannel(bg, 0);
        logoR = getChannel(logo, 16);
        logoG = getChannel(logo, 8);
        logoB = getChannel(logo, 0);

        bgMinusLogoR = bgR - logoR;
        bgMinusLogoG = bgG - logoG;
        bgMinusLogoB = bgB - logoB;
        logoMinusBgR = logoR - bgR;
        logoMinusBgG = logoG - bgG;
        logoMinusBgB = logoB - bgB;

        barWithAlpha = bar | ALPHA_MASK;
        barBgWithAlpha = barBg | ALPHA_MASK;
        borderWithAlpha = border | ALPHA_MASK;

        fadeInMs = DarkLoadingScreen.VANILLA_FADE_IN_DURATION;
        fadeOutMs = DarkLoadingScreen.VANILLA_FADE_OUT_DURATION;
    }

    private static float getChannel(int color, int offset) {
        return ((color >> offset) & 0xff) / 255f;
    }

    public int getBarColorWithAlpha(int alpha) {
        return bar | (alpha << 24);
    }

    public int getBarBgColorWithAlpha(int alpha) {
        return barBg | (alpha << 24);
    }

    public int getBorderColorWithAlpha(int alpha) {
        return border | (alpha << 24);
    }
}
