package name.skyveil.client.gui;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.map.MinimapHud;
import name.skyveil.client.pet.PetDisplayHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** One editor for every enabled Skyveil HUD. */
public final class HudEditorScreen extends Screen {
    private enum Target { NONE, MINIMAP, PET_DISPLAY }
    private Target dragging=Target.NONE;
    private boolean dirty;
    private double offsetX,offsetY;
    private HudEditorScreen(){super(Component.literal("Skyveil HUD Editor"));}
    public static void open(){Minecraft.getInstance().setScreen(new HudEditorScreen());}
    public static boolean isOpen(){return Minecraft.getInstance().screen instanceof HudEditorScreen;}

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        graphics.fill(0,0,width,height,0x55000000);
        graphics.centeredText(font,"Drag any enabled HUD - Scroll over it to resize - Esc to save",width/2,12,0xFFFFFFFF);
        var config=ConfigManager.get();
        if(config.map.enabled&&config.map.minimapEnabled)MinimapHud.renderPreview(graphics,Minecraft.getInstance());
        if(config.petDisplay.enabled)PetDisplayHud.renderPreview(graphics,Minecraft.getInstance());
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private boolean overMinimap(double x,double y){var mc=Minecraft.getInstance();var c=ConfigManager.get().map;int size=MinimapHud.displaySize(),hudX=MinimapHud.displayX(mc,size);return c.enabled&&c.minimapEnabled&&x>=hudX&&x<=hudX+size&&y>=c.minimapY&&y<=c.minimapY+size;}
    private boolean overPet(double x,double y){var mc=Minecraft.getInstance();var c=ConfigManager.get().petDisplay;var data=PetDisplayHud.previewData();return c.enabled&&x>=c.hudX&&x<=c.hudX+PetDisplayHud.contentWidth(mc,data)*c.scale&&y>=c.hudY&&y<=c.hudY+PetDisplayHud.contentHeight(data)*c.scale;}

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=0)return super.mouseClicked(event,doubleClick);
        if(overPet(event.x(),event.y())){var c=ConfigManager.get().petDisplay;dragging=Target.PET_DISPLAY;offsetX=event.x()-c.hudX;offsetY=event.y()-c.hudY;return true;}
        if(overMinimap(event.x(),event.y())){var c=ConfigManager.get().map;dragging=Target.MINIMAP;offsetX=event.x()-MinimapHud.displayX(Minecraft.getInstance(),MinimapHud.displaySize());offsetY=event.y()-c.minimapY;return true;}
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double deltaX,double deltaY){
        if(dragging!=Target.NONE)dirty=true;
        if(dragging==Target.MINIMAP){var c=ConfigManager.get().map;c.minimapX=Math.max(0,(int)(event.x()-offsetX));c.minimapY=Math.max(0,(int)(event.y()-offsetY));return true;}
        if(dragging==Target.PET_DISPLAY){var c=ConfigManager.get().petDisplay;c.hudX=Math.max(0,(int)(event.x()-offsetX));c.hudY=Math.max(0,(int)(event.y()-offsetY));return true;}
        return super.mouseDragged(event,deltaX,deltaY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging!=Target.NONE){dragging=Target.NONE;saveIfDirty();return true;}return super.mouseReleased(event);}

    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(overPet(x,y)){var mc=Minecraft.getInstance();var c=ConfigManager.get().petDisplay;var data=PetDisplayHud.previewData();double baseW=PetDisplayHud.contentWidth(mc,data),baseH=PetDisplayHud.contentHeight(data),oldW=baseW*c.scale,oldH=baseH*c.scale,rx=(x-c.hudX)/oldW,ry=(y-c.hudY)/oldH;c.scale=Math.max(.5,Math.min(2,c.scale+vertical*.1));c.hudX=Math.max(0,(int)Math.round(x-rx*baseW*c.scale));c.hudY=Math.max(0,(int)Math.round(y-ry*baseH*c.scale));dirty=true;return true;}
        if(overMinimap(x,y)){var c=ConfigManager.get().map;int oldSize=MinimapHud.displaySize(),oldX=MinimapHud.displayX(Minecraft.getInstance(),oldSize);double rx=(x-oldX)/oldSize,ry=(y-c.minimapY)/oldSize;c.minimapScale=Math.max(.5,Math.min(2,c.minimapScale+vertical*.1));int newSize=MinimapHud.displaySize();c.minimapX=Math.max(0,(int)Math.round(x-rx*newSize));c.minimapY=Math.max(0,(int)Math.round(y-ry*newSize));dirty=true;return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }

    private void saveIfDirty(){if(dirty){ConfigManager.save();dirty=false;}}
    @Override public void onClose(){saveIfDirty();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
