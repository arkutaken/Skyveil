package name.skyveil.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Gameplay HUDs are dimmed in containers; only the layout editor handles HUD input. */
public final class HudVisibility {
    private HudVisibility(){}
    public static boolean isSkillXp(String text){
        return name.skyveil.client.stats.SkillXpParser.parse(text)!=null;
    }
    /** Apply dimming to explicit sibling colors too; changing only the draw color would miss them. */
    public static net.minecraft.network.chat.Component dimText(net.minecraft.network.chat.Component text){
        if(!dimmed())return text;
        var result=net.minecraft.network.chat.Component.empty();
        for(var part:text.toFlatList()){
            var style=part.getStyle();
            if(style.getColor()!=null)style=style.withColor(color(style.getColor().getValue()));
            result.append(net.minecraft.network.chat.Component.literal(part.getString()).setStyle(style));
        }
        return result;
    }
    public static boolean dimmed(){return Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;}
    public static int color(int argb){
        if(!dimmed())return argb;
        int r=(int)(((argb>>>16)&255)*.4),g=(int)(((argb>>>8)&255)*.4),b=(int)((argb&255)*.4);
        return (argb&0xFF000000)|(r<<16)|(g<<8)|b;
    }
    public static void dimItem(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y){
        if(dimmed())graphics.fill(x,y,x+16,y+16,0x99000000);
    }
    private static net.minecraft.client.gui.GuiGraphicsExtractor pendingGraphics;
    private static final java.util.List<Runnable> pendingDraws=new java.util.ArrayList<>();
    private static final java.util.List<HudOcclusion.Rect> tooltipBounds=new java.util.ArrayList<>();

    private static void frame(net.minecraft.client.gui.GuiGraphicsExtractor graphics){
        if(pendingGraphics==graphics)return;
        pendingGraphics=graphics;pendingDraws.clear();tooltipBounds.clear();
    }

    /** Wait for this frame's tooltip layout before submitting any container HUD geometry. */
    public static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics,Runnable draw){
        if(!name.skyveil.client.SkyblockSession.isActive()||!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)){draw.run();return;}
        frame(graphics);pendingDraws.add(draw);
    }

    /** Reserve screen controls so deferred gameplay HUDs cannot draw over them. */
    public static void cover(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,int width,int height){
        frame(graphics);
        tooltipBounds.add(new HudOcclusion.Rect(x,y,x+width,y+height));
    }
    public static void tooltip(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int x,int y,int width,int height){
        frame(graphics);
        tooltipBounds.add(new HudOcclusion.Rect(x-12,y-12,x+width+12,y+height+12));
    }

    /** Called after the screen has extracted its deferred tooltip at the final scrolled position. */
    public static void finish(net.minecraft.client.gui.GuiGraphicsExtractor graphics){
        if(pendingGraphics!=graphics)return;
        try{
            if(!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen))return;
            var bounds=(name.skyveil.client.mixin.ContainerScreenAccessor)screen;
            int bottom=bounds.skyveil$getTopPos()+bounds.skyveil$getImageHeight();
            var covers=new java.util.ArrayList<>(tooltipBounds);
            boolean storage=name.skyveil.client.SkyblockSession.isActive()
                &&name.skyveil.client.storage.StoragePreviewManager.isActive(screen);
            if(storage)covers.add(new HudOcclusion.Rect(0,0,graphics.guiWidth(),Math.max(48,bottom-105)));
            covers.add(new HudOcclusion.Rect(bounds.skyveil$getLeftPos(),
                storage?bottom-101:bounds.skyveil$getTopPos(),
                bounds.skyveil$getLeftPos()+bounds.skyveil$getImageWidth()+(storage?25:0),bottom));
            var visible=HudOcclusion.visible(new HudOcclusion.Rect(0,0,graphics.guiWidth(),graphics.guiHeight()),covers);
            for(var piece:visible){
                graphics.enableScissor(piece.left(),piece.top(),piece.right(),piece.bottom());
                try{for(var draw:pendingDraws)draw.run();}finally{graphics.disableScissor();}
            }
        }finally{pendingDraws.clear();tooltipBounds.clear();pendingGraphics=null;}
    }
    /** Mining widgets yield to the expanded player list; polling continues while hidden. */
    public static boolean shouldRenderOutsidePlayerList(Minecraft client){
        return shouldRender(client)&&!client.options.keyPlayerList.isDown();
    }
    public static boolean shouldRender(Minecraft client){
        return client.player!=null&&client.level!=null&&!client.options.hideGui
            &&!(client.screen instanceof HudEditorScreen);
    }
}