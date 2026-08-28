package name.skyveil.client.pet;

import java.util.Locale;

/** Pure style palette for the equipped-pet HUD. */
enum PetDisplayStyle {
    PANEL, MINIMAL;

    static PetDisplayStyle from(String value) {
        try{return valueOf(value==null?"PANEL":value.toUpperCase(Locale.ROOT));}
        catch(IllegalArgumentException ignored){return PANEL;}
    }

    Palette palette(int rarityRgb,int alpha) {
        int rarity=rarityRgb&0xFFFFFF;
        return switch(this) {
            case PANEL -> new Palette(true,(alpha<<24)|0x201730,(alpha<<24)|0x9B6CFF,0xFF9B6CFF,false);
            case MINIMAL -> new Palette(false,0,0,0xFF000000|rarity,false);
        };
    }

    record Palette(boolean background,int backgroundColor,int outlineColor,int progressColor,boolean accent) {}
}
