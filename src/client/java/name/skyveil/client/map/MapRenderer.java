package name.skyveil.client.map;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import name.skyveil.client.map.MapCoordinateTransformer.View;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Shared, texture-independent rendering for minimap and full-map views. */
public final class MapRenderer {
    private static final int GRID=32;
    private MapRenderer() {}

    public static View fullView(SkyblockMapDefinition map,int x,int y,int width,int height,double zoom,double panX,double panY){
        double fit=Math.min(width/(map.maxX-map.minX),height/(map.maxZ-map.minZ));
        return new View(x,y,width,height,map.centerX(),map.centerZ(),fit*zoom,panX,panY);
    }
    public static View minimapView(int x,int y,int size,double playerX,double playerZ,double blocksPerPixel){
        return new View(x,y,size,size,playerX,playerZ,1.0/blocksPerPixel,0,0);
    }

    public static void draw(GuiGraphicsExtractor g,Minecraft client,SkyblockMapDefinition map,View view,int backgroundAlpha,
                            boolean showZones,boolean showNpcs,boolean showCustom,boolean showPlayer,boolean showDirection,boolean labels){
        int alpha=Math.max(0,Math.min(255,backgroundAlpha));Identifier mapTexture=MapTextureManager.texture(map);
        g.fill(view.x(),view.y(),view.x()+view.width(),view.y()+view.height(),0xEE100B18);
        if(mapTexture==null){g.outline(view.x(),view.y(),view.width(),view.height(),0xCC9B6CFF);g.centeredText(client.font,MapTextureManager.status(map),view.x()+view.width()/2,view.y()+view.height()/2-4,0xFFE0D7EC);return;}
        g.enableScissor(view.x(),view.y(),view.x()+view.width(),view.y()+view.height());
        drawTexture(g,map,view,mapTexture,alpha);
        if(name.skyveil.client.config.ConfigManager.get().map.debug)drawGrid(g,client,map,view,true);
        if(showZones)for(var marker:map.zones)drawStatic(g,client,view,marker,0xFFB9A0E8,labels);
        if(showCustom)for(var marker:MapMarkerManager.forMap(map.id))drawCustom(g,client,view,marker,labels);
        if(showNpcs)for(var marker:map.npcs)drawNpc(g,client,view,marker,labels);
        if(showPlayer&&client.player!=null)drawPlayer(g,client,view,client.player.getX(),client.player.getZ(),client.player.getYRot(),showDirection);
        g.disableScissor();
        g.outline(view.x(),view.y(),view.width(),view.height(),0xCC9B6CFF);
    }

    private static void drawTexture(GuiGraphicsExtractor g,SkyblockMapDefinition map,View view,Identifier texture,int alpha){
        int left=(int)Math.round(MapCoordinateTransformer.screenX(view,map.minX));
        int top=(int)Math.round(MapCoordinateTransformer.screenY(view,map.minZ));
        int width=Math.max(1,(int)Math.round((map.maxX-map.minX)*view.pixelsPerBlock()));
        int height=Math.max(1,(int)Math.round((map.maxZ-map.minZ)*view.pixelsPerBlock()));
        g.blit(RenderPipelines.GUI_TEXTURED,texture,left,top,0,0,width,height,map.imageWidth,map.imageHeight,map.imageWidth,map.imageHeight,(alpha<<24)|0xFFFFFF);
    }

    private static void drawGrid(GuiGraphicsExtractor g,Minecraft client,SkyblockMapDefinition map,View view,boolean labels){
        double left=MapCoordinateTransformer.worldX(view,view.x()),right=MapCoordinateTransformer.worldX(view,view.x()+view.width());
        double top=MapCoordinateTransformer.worldZ(view,view.y()),bottom=MapCoordinateTransformer.worldZ(view,view.y()+view.height());
        int startX=(int)Math.floor(Math.max(map.minX,left)/GRID)*GRID;
        int endX=(int)Math.ceil(Math.min(map.maxX,right)/GRID)*GRID;
        int startZ=(int)Math.floor(Math.max(map.minZ,top)/GRID)*GRID;
        int endZ=(int)Math.ceil(Math.min(map.maxZ,bottom)/GRID)*GRID;
        for(int worldX=startX;worldX<=endX;worldX+=GRID){int sx=(int)Math.round(MapCoordinateTransformer.screenX(view,worldX));g.fill(sx,view.y(),sx+1,view.y()+view.height(),worldX==0?0x887D5BA6:0x383F3155);if(labels&&view.pixelsPerBlock()>.55)g.text(client.font,Integer.toString(worldX),sx+2,view.y()+2,0x999E91B2,false);}
        for(int worldZ=startZ;worldZ<=endZ;worldZ+=GRID){int sy=(int)Math.round(MapCoordinateTransformer.screenY(view,worldZ));g.fill(view.x(),sy,view.x()+view.width(),sy+1,worldZ==0?0x887D5BA6:0x383F3155);if(labels&&view.pixelsPerBlock()>.55)g.text(client.font,Integer.toString(worldZ),view.x()+2,sy+2,0x999E91B2,false);}
    }
    private static void drawStatic(GuiGraphicsExtractor g,Minecraft client,View view,SkyblockMapDefinition.StaticMarker marker,int color,boolean label){
        int x=(int)Math.round(MapCoordinateTransformer.screenX(view,marker.x)),y=(int)Math.round(MapCoordinateTransformer.screenY(view,marker.z));
        g.fill(x-2,y-2,x+3,y+3,0xDD000000);g.fill(x-1,y-1,x+2,y+2,color);
        if(label)g.text(client.font,marker.name,x+4,y-4,color,true);
    }
    private static void drawNpc(GuiGraphicsExtractor g,Minecraft client,View view,SkyblockMapDefinition.StaticMarker marker,boolean label){
        int x=(int)Math.round(MapCoordinateTransformer.screenX(view,marker.x)),y=(int)Math.round(MapCoordinateTransformer.screenY(view,marker.z));
        int size=(int)Math.round(name.skyveil.client.config.ConfigManager.get().map.npcMarkerSize);
        Identifier skin=NpcSkinCache.skin(marker.name);if(skin==null)skin=DefaultPlayerSkin.getDefaultTexture();drawHead(g,skin,x,y,size);
        if(label)g.text(client.font,marker.name,x+size/2+2,y-4,0xFFFFD166,true);
    }
    private static void drawCustom(GuiGraphicsExtractor g,Minecraft client,View view,CustomMapMarker marker,boolean label){
        int x=(int)Math.round(MapCoordinateTransformer.screenX(view,marker.x())),y=(int)Math.round(MapCoordinateTransformer.screenY(view,marker.z()));
        g.fill(x-3,y,x+4,y+1,0xFFFFFFFF);g.fill(x,y-3,x+1,y+4,0xFFFFFFFF);
        if(label)g.text(client.font,marker.name(),x+4,y-4,0xFFFFFFFF,true);
    }
    private static void drawPlayer(GuiGraphicsExtractor g,Minecraft client,View view,double worldX,double worldZ,float yaw,boolean direction){
        int x=(int)Math.round(MapCoordinateTransformer.screenX(view,worldX)),y=(int)Math.round(MapCoordinateTransformer.screenY(view,worldZ));
        int size=(int)Math.round(name.skyveil.client.config.ConfigManager.get().map.playerMarkerSize);
        Identifier skin=client.player.getSkin().body().texturePath();drawHead(g,skin,x,y,size);
        if(direction){String marker=direction(yaw);g.centeredText(client.font,marker,x,y+size/2-2,0xFF55FFFF);}
    }
    private static void drawHead(GuiGraphicsExtractor g,Identifier skin,int centerX,int centerY,int size){
        int x=centerX-size/2,y=centerY-size/2;g.fill(x-1,y-1,x+size+1,y+size+1,0xDD000000);
        g.blit(RenderPipelines.GUI_TEXTURED,skin,x,y,8,8,size,size,8,8,64,64);
        g.blit(RenderPipelines.GUI_TEXTURED,skin,x,y,40,8,size,size,8,8,64,64);
    }
    private static String direction(float yaw){
        String[] arrows={"v","/","<","\\","^","/",">","\\"};
        return arrows[Math.floorMod(Math.round(yaw/45.0f),8)];
    }

    public static CustomMapMarker customMarkerAt(SkyblockMapDefinition map,View view,double mouseX,double mouseY,double radius){
        for(CustomMapMarker marker:MapMarkerManager.forMap(map.id)){
            double x=MapCoordinateTransformer.screenX(view,marker.x()),y=MapCoordinateTransformer.screenY(view,marker.z());
            if(Math.abs(mouseX-x)<=radius&&Math.abs(mouseY-y)<=radius)return marker;
        }
        return null;
    }
    public static SkyblockMapDefinition.StaticMarker staticMarkerAt(List<SkyblockMapDefinition.StaticMarker> markers,View view,double mouseX,double mouseY,double radius){
        for(var marker:markers){double x=MapCoordinateTransformer.screenX(view,marker.x),y=MapCoordinateTransformer.screenY(view,marker.z);if(Math.abs(mouseX-x)<=radius&&Math.abs(mouseY-y)<=radius)return marker;}
        return null;
    }
}
