package name.skyveil.client.map;

import name.skyveil.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MinimapHud {
    private MinimapHud() {}
    public static void register(){HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("minimap"),(graphics,delta)->render(graphics));}
    private static void render(GuiGraphicsExtractor graphics){
        if(name.skyveil.client.gui.HudEditorScreen.isOpen())return;
        var config=ConfigManager.get().map;var client=Minecraft.getInstance();var map=MapManager.current();
        if(!config.enabled||!config.minimapEnabled||client.options.hideGui||client.player==null||map==null)return;
        int size=displaySize(),x=displayX(client,size),y=config.minimapY;
        renderContents(graphics,client,map,x,y,size,client.player.getX(),client.player.getZ());
    }
    public static int displaySize(){var c=ConfigManager.get().map;return (int)Math.round(c.minimapSize*c.minimapScale);}
    public static int displayX(Minecraft client,int size){int configured=ConfigManager.get().map.minimapX;return configured<0?Math.max(4,client.getWindow().getGuiScaledWidth()-size-8):configured;}
    public static void renderPreview(GuiGraphicsExtractor graphics,Minecraft client){
        var map=SkyblockMapRegistry.byId("hub");if(map==null)return;int size=displaySize();renderContents(graphics,client,map,displayX(client,size),ConfigManager.get().map.minimapY,size,-2,-69);
    }
    public static void renderContents(GuiGraphicsExtractor graphics,Minecraft client,SkyblockMapDefinition map,int x,int y,int size,double playerX,double playerZ){
        var c=ConfigManager.get().map;var view=MapRenderer.minimapView(x,y,size,playerX,playerZ,c.minimapZoom);
        MapRenderer.draw(graphics,client,map,view,(int)Math.round(c.minimapOpacity*255),false,c.npcsEnabled&&c.minimapNpcs,c.minimapCustom,true,c.minimapDirection,false);
        graphics.text(client.font,map.displayName,x+4,y+4,0xFFF5F2FF,true);
    }
}
