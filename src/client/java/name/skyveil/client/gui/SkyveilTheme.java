package name.skyveil.client.gui;

/** Shared Commissions-style palette for Skyveil panels, HUDs and settings. */
public final class SkyveilTheme {
    // Packed ARGB colors: HUD panels are translucent; text and control surfaces
    // generally use full alpha. Share these values instead of per-module palettes.
    // Neutral grays keep panels free of color casts; orange marks active controls.
    public static final int HUD_BACKGROUND=0x99181818,BAR_TRACK=0xFF383838;
    public static final int SCRIM=0x77000000,WINDOW=0xF0181818,WINDOW_TOP=0xFF181818,SIDEBAR=0xE0181818,PANEL=0xFF212121;
    public static final int CARD=0xFF252525,CARD_ALT=0xFF202020,HOVER=0xFF383838,ACCENT=0xFFFFAA00,ACCENT_DARK=0xFF665022;
    public static final int TEXT=0xFFFFFFFF,SECONDARY=0xFFAAAAAA,MUTED=0xFF888888,OUTLINE=BAR_TRACK,OFF=0xFF444450,SUCCESS=0xFF55FF55;
    /** Kept as the settings renderer's palette API; legacy theme choices share this style. */
    public static final class ConfigPalette {
        public final int SCRIM=SkyveilTheme.SCRIM,WINDOW=SkyveilTheme.WINDOW,WINDOW_TOP=SkyveilTheme.WINDOW_TOP,
            SIDEBAR=SkyveilTheme.SIDEBAR,PANEL=SkyveilTheme.PANEL,CARD=SkyveilTheme.CARD,CARD_ALT=SkyveilTheme.CARD_ALT,
            HOVER=SkyveilTheme.HOVER,ACCENT=SkyveilTheme.ACCENT,ACCENT_DARK=SkyveilTheme.ACCENT_DARK,
            TEXT=SkyveilTheme.TEXT,SECONDARY=SkyveilTheme.SECONDARY,MUTED=SkyveilTheme.MUTED,
            OUTLINE=SkyveilTheme.OUTLINE,OFF=SkyveilTheme.OFF,SUCCESS=SkyveilTheme.SUCCESS;
        private ConfigPalette(){}
    }
    private static final ConfigPalette CONFIG=new ConfigPalette();
    public static ConfigPalette config(boolean legacyBlack){return CONFIG;}
    private SkyveilTheme(){}
}
