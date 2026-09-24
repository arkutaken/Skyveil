package name.skyveil.client.gui;

import name.skyveil.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A read-only view of the 27 main inventory slots, read directly each frame. */
public final class InventoryPreviewHud {
    public static final int WIDTH=188,HEIGHT=68;
    private InventoryPreviewHud(){}
    public static void register(){
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("inventory_preview"),(g,delta)->{
            if(name.skyveil.client.SkyblockSession.isActive()&&ConfigManager.get().inventoryPreview.enabled&&HudVisibility.shouldRender(Minecraft.getInstance()))HudVisibility.render(g,()->render(g));
        });
    }
    public static int x(int width){
        var c=ConfigManager.get().inventoryPreview;
        return Math.max(0,Math.min(c.hudX<0?(int)((width-WIDTH*c.scale)/2):c.hudX,(int)(width-WIDTH*c.scale)));
    }
    public static int y(int height){
        var c=ConfigManager.get().inventoryPreview;
        return Math.max(0,Math.min(c.hudY<0?height-230:c.hudY,(int)(height-HEIGHT*c.scale)));
    }
    public static void render(GuiGraphicsExtractor g){
        var client=Minecraft.getInstance();var c=ConfigManager.get().inventoryPreview;
        g.pose().pushMatrix();
        try{
            g.pose().translate(x(g.guiWidth()),y(g.guiHeight()));
            g.pose().scale((float)c.scale,(float)c.scale);
            int alpha=(int)Math.round(c.backgroundOpacity*255);
            int rgb=SkyveilTheme.HUD_BACKGROUND&0xFFFFFF;
            g.fill(0,0,WIDTH,HEIGHT,(alpha<<24)|rgb);

            for(int row=0;row<3;row++)for(int column=0;column<9;column++){
                int sx=5+column*20,sy=5+row*20;
                
                if(client.player==null)continue;
                var stack=client.player.getInventory().getItem(9+row*9+column);
                if(stack.isEmpty())continue;
                g.item(stack,sx,sy);
                g.itemDecorations(client.font,stack,sx,sy);

            }
            if(HudVisibility.dimmed())g.fill(0,0,WIDTH,HEIGHT,0x66000000);
        }finally{g.pose().popMatrix();}
    }
}