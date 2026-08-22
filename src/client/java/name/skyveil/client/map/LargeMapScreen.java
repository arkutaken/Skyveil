package name.skyveil.client.map;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Stationary full-island coordinate view; player and marker positions move over it. */
public final class LargeMapScreen extends Screen {
    private double zoom=1,panX,panY;
    private boolean dragging;
    private MapCoordinateTransformer.View lastView;
    private SkyblockMapDefinition lastMap;
    public LargeMapScreen(){super(Component.literal("Skyveil Map"));}
    public static void open(){Minecraft.getInstance().setScreen(new LargeMapScreen());}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xB0100B18);
        SkyblockMapDefinition map=MapManager.current();lastMap=map;
        g.text(font,"SKYVEIL MAP",12,10,0xFFB58AFF,true);
        drawButton(g,width-54,7,42,18,"Close",mouseX,mouseY);
        drawButton(g,width-150,7,88,18,"Reset View",mouseX,mouseY);
        drawButton(g,12,height-27,82,18,"Add Marker",mouseX,mouseY);
        if(map==null){g.centeredText(font,"Map unavailable for this area",width/2,height/2-5,0xFFE0D7EC);g.centeredText(font,"No verified local definition is registered.",width/2,height/2+10,0xFF9E91B2);lastView=null;super.extractRenderState(g,mouseX,mouseY,partial);return;}
        Rect rect=mapRect(map);lastView=MapRenderer.fullView(map,rect.x,rect.y,rect.w,rect.h,zoom,panX,panY);
        var c=ConfigManager.get().map;
        MapRenderer.draw(g,Minecraft.getInstance(),map,lastView,255,c.largeZones,c.npcsEnabled&&c.largeNpcs,c.largeCustom,c.largePlayer,true,true);
        g.text(font,map.displayName,rect.x+5,rect.y+5,0xFFF5F2FF,true);
        g.text(font,String.format(Locale.ROOT,"Zoom %.1fx",zoom),rect.x+5,rect.y+17,0xFFB9A0E8,false);
        if(c.largeCoordinates&&minecraft.player!=null)g.text(font,String.format(Locale.ROOT,"X %.1f  Y %.1f  Z %.1f",minecraft.player.getX(),minecraft.player.getY(),minecraft.player.getZ()),102,height-23,0xFFF5F2FF,true);
        if(c.debug&&minecraft.player!=null){double px=MapCoordinateTransformer.normalizedX(map,minecraft.player.getX())*map.imageWidth,pz=MapCoordinateTransformer.normalizedZ(map,minecraft.player.getZ())*map.imageHeight;g.text(font,String.format(Locale.ROOT,"DEBUG %s | image %dx%d | pixel %.1f, %.1f | bounds X %.0f..%.0f Z %.0f..%.0f",map.id,map.imageWidth,map.imageHeight,px,pz,map.minX,map.maxX,map.minZ,map.maxZ),rect.x+5,rect.y+29,0xFFFFFF55,true);}
        showHover(g,map,lastView,mouseX,mouseY,c);
        super.extractRenderState(g,mouseX,mouseY,partial);
    }
    private Rect mapRect(SkyblockMapDefinition map){
        double scale=ConfigManager.get().map.largeMapScale;int maxW=(int)((width-32)*scale),maxH=(int)((height-62)*scale);
        double ratio=(map.maxX-map.minX)/(map.maxZ-map.minZ);int w=maxW,h=(int)Math.round(w/ratio);if(h>maxH){h=maxH;w=(int)Math.round(h*ratio);}return new Rect((width-w)/2,34,w,h);
    }
    private void showHover(GuiGraphicsExtractor g,SkyblockMapDefinition map,MapCoordinateTransformer.View view,int mx,int my,name.skyveil.client.config.SkyveilConfig.MapSettings c){
        if(c.largeCustom){var custom=MapRenderer.customMarkerAt(map,view,mx,my,6);if(custom!=null){g.setComponentTooltipForNextFrame(font,List.of(Component.literal(custom.name()),Component.literal(coords(custom.x(),custom.y(),custom.z())),Component.literal("Left-click to edit")),mx,my);return;}}
        if(!c.npcTooltips)return;
        SkyblockMapDefinition.StaticMarker marker=c.npcsEnabled&&c.largeNpcs?MapRenderer.staticMarkerAt(map.npcs,view,mx,my,c.npcMarkerSize/2+2):null;
        if(marker==null&&c.largeZones)marker=MapRenderer.staticMarkerAt(map.zones,view,mx,my,5);
        if(marker!=null){var lines=new java.util.ArrayList<Component>();lines.add(Component.literal(marker.name));if(!marker.description.isBlank())lines.add(Component.literal(marker.description));if(c.npcTooltipCoordinates||!map.npcs.contains(marker))lines.add(Component.literal(coords(marker.x,marker.y,marker.z)));g.setComponentTooltipForNextFrame(font,lines,mx,my);}
    }
    private static String coords(double x,double y,double z){return String.format(Locale.ROOT,"X %.1f  Y %.1f  Z %.1f",x,y,z);}
    private void drawButton(GuiGraphicsExtractor g,int x,int y,int w,int h,String label,int mx,int my){boolean over=inside(mx,my,x,y,w,h);g.fill(x,y,x+w,y+h,over?0xDD7953C8:0xBB3F3155);g.centeredText(font,label,x+w/2,y+5,0xFFF5F2FF);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        double x=event.x(),y=event.y();
        if(event.button()==0&&inside(x,y,width-54,7,42,18)){onClose();return true;}
        if(event.button()==0&&inside(x,y,width-150,7,88,18)){zoom=1;panX=panY=0;return true;}
        if(event.button()==0&&inside(x,y,12,height-27,82,18)){openEditorAtPlayer();return true;}
        if(lastMap==null||lastView==null)return super.mouseClicked(event,twice);
        if(event.button()==0){var marker=MapRenderer.customMarkerAt(lastMap,lastView,x,y,7);if(marker!=null){minecraft.setScreen(new MarkerEditScreen(this,lastMap,marker,marker.x(),marker.y(),marker.z()));return true;}dragging=true;return true;}
        if(event.button()==1&&inside(x,y,lastView.x(),lastView.y(),lastView.width(),lastView.height())){double wx=MapCoordinateTransformer.worldX(lastView,x),wz=MapCoordinateTransformer.worldZ(lastView,y),wy=minecraft.player==null?0:minecraft.player.getY();minecraft.setScreen(new MarkerEditScreen(this,lastMap,null,wx,wy,wz));return true;}
        return super.mouseClicked(event,twice);
    }
    private void openEditorAtPlayer(){if(lastMap==null)return;double x=0,y=0,z=0;if(minecraft.player!=null){x=minecraft.player.getX();y=minecraft.player.getY();z=minecraft.player.getZ();}minecraft.setScreen(new MarkerEditScreen(this,lastMap,null,x,y,z));}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(dragging&&zoom>1){panX+=dx;panY+=dy;clampPan();return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging){dragging=false;return true;}return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(lastView!=null&&inside(x,y,lastView.x(),lastView.y(),lastView.width(),lastView.height())){zoom=Math.max(1,Math.min(5,zoom+vertical*.25));if(zoom==1)panX=panY=0;clampPan();return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    private void clampPan(){if(lastView==null)return;double maxX=lastView.width()*(zoom-1)/2,maxY=lastView.height()*(zoom-1)/2;panX=Math.max(-maxX,Math.min(maxX,panX));panY=Math.max(-maxY,Math.min(maxY,panY));}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==ConfigManager.get().map.largeMapKey){onClose();return true;}return super.keyPressed(event);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private record Rect(int x,int y,int w,int h){}
}
