package name.skyveil.client.itemrarity;

/** Official SkyBlock rarity labels and their Minecraft formatting colors. */
public enum SkyblockRarity {
    COMMON("COMMON",0xFFFFFF),
    UNCOMMON("UNCOMMON",0x55FF55),
    RARE("RARE",0x5555FF),
    EPIC("EPIC",0xAA00AA),
    LEGENDARY("LEGENDARY",0xFFAA00),
    MYTHIC("MYTHIC",0xFF55FF),
    DIVINE("DIVINE",0x55FFFF),
    SPECIAL("SPECIAL",0xFF5555),
    VERY_SPECIAL("VERY SPECIAL",0xFF5555),
    SUPREME("SUPREME",0xAA0000),
    ULTIMATE("ULTIMATE",0xAA0000),
    ADMIN("ADMIN",0xAA0000);

    private final String displayName;
    private final int rgb;

    SkyblockRarity(String displayName,int rgb) {
        this.displayName=displayName;
        this.rgb=rgb;
    }

    public int rgb(){return rgb;}

    public static SkyblockRarity fromLabel(String raw) {
        if(raw==null)return null;
        String value=raw.toUpperCase(java.util.Locale.ROOT).replace('_',' ').replaceAll("\\s+"," ").trim();
        if(value.equals("LEGENJERRY"))return LEGENDARY;
        for(SkyblockRarity rarity:values())if(rarity.displayName.equals(value))return rarity;
        return null;
    }

    /** Maps Hypixel's actual Minecraft text color back to the shared rarity palette. */
    public static SkyblockRarity fromRgb(int rawRgb) {
        int value=rawRgb&0xFFFFFF;
        // Hypixel sometimes renders COMMON item names in gray rather than white.
        if(value==0xAAAAAA)return COMMON;
        for(SkyblockRarity rarity:values())if(rarity.rgb==value)return rarity;
        return null;
    }
}
