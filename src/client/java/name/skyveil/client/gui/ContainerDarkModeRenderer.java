package name.skyveil.client.gui;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lightweight tint over the currently resolved container texture, before slots/items are extracted. */
public final class ContainerDarkModeRenderer {
    private ContainerDarkModeRenderer() {}

    public static void render(GuiGraphicsExtractor graphics,int left,int top,int width,int height){
        if(!SkyblockSession.isActive())return;
        int tint=switch(ConfigManager.get().darkMode){
            case "DARK"->0xA814171C;
            case "DARK_PURPLE"->0xB810091A;
            default->0;
        };
        if(tint!=0)graphics.fill(left,top,left+width,top+height,tint);
    }

    public static int labelColor(int vanilla){return !SkyblockSession.isActive()||"DEFAULT".equals(ConfigManager.get().darkMode)?vanilla:0xFFF4F2F7;}

    /** Opaque companion colors for custom controls drawn beside the tinted vanilla GUI. */
    public static ControlPalette controlPalette(){return controlPalette(ConfigManager.get().darkMode);}
    static ControlPalette controlPalette(String mode){return switch(mode){
        case "DARK"->new ControlPalette(0xFF505258,0xFF3C3F44,0xFF65686E,0xFF272A30,0xFFAAB2C0);
        case "DARK_PURPLE"->new ControlPalette(0xFF463E4A,0xFF343039,0xFF5B5062,0xFF2D2533,0xFF9B6CFF);
        default->new ControlPalette(0xFFC6C6C6,0xFF8B8B8B,0xFFD4D4D4,0xFF555555,0xFFFFFFFF);
    };}

    public record ControlPalette(int panel,int slot,int hover,int outline,int accent){}

}
