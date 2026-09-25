package name.skyveil.client.gui;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.pet.PetDisplayHud;
import name.skyveil.client.performance.PerformanceHud;
import name.skyveil.client.stats.PlayerStat;
import name.skyveil.client.stats.SkillXpHud;
import name.skyveil.client.stats.PlayerStatsHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/** Shared movement, resizing, and alignment controls for every enabled Skyveil HUD. */
public final class HudEditorScreen extends Screen {
    private static final int HANDLE=7,GUIDE=SkyveilTheme.ACCENT;
    private final List<Element> elements=new ArrayList<>();
    private Element selected,dragging;
    private boolean resizing,dirty;
    private double offsetX,offsetY,startMouseX,startMouseY,startScale;
    private Double verticalGuide,horizontalGuide;

    @FunctionalInterface private interface LayoutSetter {void set(int x,int y,double scale);}
    // Suppliers keep hit boxes current when text widths change. Width/height are
    // unscaled GUI units; bounds applies scale exactly once. settingKey is the
    // SettingsRegistry target opened by a right-click.
    private record Element(String name,String settingKey,IntSupplier x,IntSupplier y,IntSupplier width,IntSupplier height,
                           DoubleSupplier scale,LayoutSetter setter){
        HudAlignment.Rect bounds(){return new HudAlignment.Rect(x.getAsInt(),y.getAsInt(),width.getAsInt()*scale.getAsDouble(),height.getAsInt()*scale.getAsDouble());}
    }
    private HudEditorScreen(){super(Component.literal("Skyveil HUD Editor"));}
    public static void open(){Minecraft.getInstance().setScreen(new HudEditorScreen());}
    public static boolean isOpen(){return Minecraft.getInstance().screen instanceof HudEditorScreen;}

    @Override protected void init(){
        super.init();
        dragging=null;selected=null;verticalGuide=null;horizontalGuide=null;elements.clear();
        var config=ConfigManager.get();var client=Minecraft.getInstance();
        if(config.petDisplay.enabled){
            var c=config.petDisplay;
            elements.add(new Element("Pet Display","hud.petDisplay.enabled",()->c.hudX,()->c.hudY,
                ()->PetDisplayHud.contentWidth(client,PetDisplayHud.previewData()),()->PetDisplayHud.contentHeight(PetDisplayHud.previewData()),
                ()->c.scale,(x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.performance.enabled){
            var c=config.performance;
            elements.add(new Element("Performance","hud.performance.enabled",()->c.hudX,()->c.hudY,
                ()->PerformanceHud.contentWidth(client),PerformanceHud::contentHeight,
                ()->c.scale,(x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.inventoryPreview.enabled){
            var c=config.inventoryPreview;
            elements.add(new Element("Inventory Preview","hud.inventoryPreview.enabled",()->InventoryPreviewHud.x(width),()->InventoryPreviewHud.y(height),
                ()->InventoryPreviewHud.WIDTH,()->InventoryPreviewHud.HEIGHT,()->c.scale,
                (x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.crystalHollowsMap.enabled){
            var c=config.crystalHollowsMap;
            elements.add(new Element("Crystal Hollows Map","mining.crystalHollowsMap.enabled",
                ()->name.skyveil.client.mining.CrystalHollowsMapHud.x(width),()->name.skyveil.client.mining.CrystalHollowsMapHud.y(height),
                ()->name.skyveil.client.mining.CrystalHollowsMapHud.WIDTH,()->name.skyveil.client.mining.CrystalHollowsMapHud.HEIGHT,()->c.scale,
                (x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.pickaxeAbility.enabled){
            var c=config.pickaxeAbility;
            elements.add(new Element("Pickaxe Ability","mining.pickaxeAbility.enabled",()->name.skyveil.client.mining.PickaxeAbilityHud.x(width,true),()->name.skyveil.client.mining.PickaxeAbilityHud.y(height),
                ()->name.skyveil.client.mining.PickaxeAbilityHud.width(true),()->name.skyveil.client.mining.PickaxeAbilityHud.height(),()->c.scale,
                (x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.commissions.enabled){
            var c=config.commissions;
            elements.add(new Element("Commissions","mining.commissions.enabled",()->name.skyveil.client.mining.CommissionsHud.x(width),()->name.skyveil.client.mining.CommissionsHud.y(height),
                ()->name.skyveil.client.mining.CommissionsHud.WIDTH,()->name.skyveil.client.mining.CommissionsHud.HEIGHT,()->c.scale,
                (x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.skillXp.enabled){
            var c=config.skillXp;
            elements.add(new Element("Skill XP","hud.skillXp.enabled",()->SkillXpHud.x(width),()->SkillXpHud.y(height),
                ()->SkillXpHud.WIDTH,()->SkillXpHud.HEIGHT,()->c.scale,(x,y,s)->{c.hudX=x;c.hudY=y;c.scale=s;}));
        }
        if(config.playerStats.enabled)for(var stat:PlayerStat.values()){
            var p=PlayerStatsHud.position(stat);
            elements.add(new Element(stat.label,"hud.playerStats.enabled",()->PlayerStatsHud.x(stat,width),()->PlayerStatsHud.y(stat,height),
                ()->PlayerStatsHud.contentWidth(stat),()->PlayerStatsHud.contentHeight(stat),()->p.scale,(x,y,s)->{p.hudX=x;p.hudY=y;p.scale=s;}));
        }
        for(var element:elements){
            var b=element.bounds();var clamped=HudAlignment.move(b,List.of(),width,height,false);
            if(clamped.x()!=b.x()||clamped.y()!=b.y())apply(element,clamped.x(),clamped.y(),element.scale.getAsDouble());
        }
        addRenderableWidget(new SkyveilButton(width/2-104,26,132,20,snapLabel(),button->{
            config.hudAlignmentSnap=!config.hudAlignmentSnap;dirty=true;
            verticalGuide=null;horizontalGuide=null;button.setMessage(snapLabel());saveIfDirty();
        }));
        addRenderableWidget(new SkyveilButton(width/2+34,26,70,20,Component.literal("Done"),button->onClose()));
    }

    private Component snapLabel(){return Component.literal("Alignment: "+(ConfigManager.get().hudAlignmentSnap?"ON":"OFF"));}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        g.fill(0,0,width,height,0x55000000);
        var config=ConfigManager.get();var client=Minecraft.getInstance();
        if(config.petDisplay.enabled)PetDisplayHud.renderPreview(g,client);
        if(config.performance.enabled)PerformanceHud.renderPreview(g,client);
        if(config.inventoryPreview.enabled)InventoryPreviewHud.render(g);
        if(config.skillXp.enabled)SkillXpHud.render(g,true);
        if(config.commissions.enabled)name.skyveil.client.mining.CommissionsHud.render(g,true);
        if(config.crystalHollowsMap.enabled)name.skyveil.client.mining.CrystalHollowsMapHud.render(g,true);
        if(config.pickaxeAbility.enabled)name.skyveil.client.mining.PickaxeAbilityHud.render(g,true);
        if(config.playerStats.enabled)for(var stat:PlayerStat.values())PlayerStatsHud.render(g,stat,true);
        for(var element:elements){
            var b=element.bounds();
            int x=(int)b.x(),y=(int)b.y(),w=(int)Math.round(b.width()),h=(int)Math.round(b.height());
            int color=element==selected?GUIDE:0xAA999999;
            g.outline(x-1,y-1,w+2,h+2,color);
            int hx=handleX(b),hy=handleY(b);
            g.fill(hx,hy,hx+HANDLE,hy+HANDLE,color);
            g.fill(hx+2,hy+2,hx+HANDLE-2,hy+HANDLE-2,0xFF202020);
        }
        if(verticalGuide!=null){int x=(int)Math.round(verticalGuide);g.fill(x,0,x+1,height,GUIDE);}
        if(horizontalGuide!=null){int y=(int)Math.round(horizontalGuide);g.fill(0,y,width,y+1,GUIDE);}
        g.fill(width/2-108,24,width/2+108,48,0xB0101010);
        g.centeredText(font,"Drag: move - Corner/scroll: resize - Right-click: settings",width/2,10,0xFFFFFFFF);
        if(selected!=null){
            var b=selected.bounds();
            g.centeredText(font,selected.name+"  "+(int)b.x()+", "+(int)b.y()+"  "+String.format(java.util.Locale.ROOT,"%.1f",selected.scale.getAsDouble()*100)+"%",width/2,height-12,GUIDE);
        }
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }

    private int handleX(HudAlignment.Rect b){return (int)Math.min(width-HANDLE,Math.round(b.x()+b.width())+2);}
    private int handleY(HudAlignment.Rect b){return (int)Math.min(height-HANDLE,Math.round(b.y()+b.height())+2);}
    private boolean onHandle(HudAlignment.Rect b,double x,double y){
        return x>=handleX(b)&&x<=handleX(b)+HANDLE&&y>=handleY(b)&&y<=handleY(b)+HANDLE;
    }
    private Element at(double x,double y){
        for(int i=elements.size()-1;i>=0;i--)if(onHandle(elements.get(i).bounds(),x,y))return elements.get(i);
        for(int i=elements.size()-1;i>=0;i--){
            var element=elements.get(i);var b=element.bounds();
            if(x>=b.x()&&x<=b.x()+b.width()&&y>=b.y()&&y<=b.y()+b.height())return element;
        }
        return null;
    }
    private List<HudAlignment.Rect> otherBounds(Element moving){
        return elements.stream().filter(element->element!=moving).map(Element::bounds).toList();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(super.mouseClicked(event,doubleClick))return true;
        if(event.button()==1){
            var element=at(event.x(),event.y());
            if(element==null)return false;
            dragging=null;resizing=false;verticalGuide=null;horizontalGuide=null;
            saveIfDirty();
            SkyveilConfigScreen.open(element.settingKey);
            return true;
        }
        if(event.button()!=0)return false;
        selected=at(event.x(),event.y());dragging=selected;
        verticalGuide=null;horizontalGuide=null;
        if(dragging==null)return false;
        setFocused(null);
        var b=dragging.bounds();
        offsetX=event.x()-b.x();offsetY=event.y()-b.y();
        resizing=onHandle(b,event.x(),event.y());
        startMouseX=event.x();startMouseY=event.y();startScale=dragging.scale.getAsDouble();
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(dragging==null)return super.mouseDragged(event,dx,dy);
        var b=dragging.bounds();
        if(resizing){
            double scale=HudAlignment.resizeScale(startScale,event.x()-startMouseX,event.y()-startMouseY,
                dragging.width.getAsInt(),dragging.height.getAsInt());
            var sized=HudAlignment.resize(b.x(),b.y(),dragging.width.getAsInt(),dragging.height.getAsInt(),scale,
                otherBounds(dragging),width,height,ConfigManager.get().hudAlignmentSnap);
            resize(dragging,sized.scale(),b.x(),b.y());
            verticalGuide=sized.verticalGuide();horizontalGuide=sized.horizontalGuide();
        }else{
            var placement=HudAlignment.move(new HudAlignment.Rect(event.x()-offsetX,event.y()-offsetY,b.width(),b.height()),
                otherBounds(dragging),width,height,ConfigManager.get().hudAlignmentSnap);
            apply(dragging,placement.x(),placement.y(),dragging.scale.getAsDouble());
            verticalGuide=placement.verticalGuide();horizontalGuide=placement.horizontalGuide();
        }
        return true;
    }

    private void resize(Element element,double scale,double x,double y){
        var placement=HudAlignment.move(new HudAlignment.Rect(x,y,element.width.getAsInt()*scale,element.height.getAsInt()*scale),
            List.of(),width,height,false);
        apply(element,placement.x(),placement.y(),scale);
        verticalGuide=null;horizontalGuide=null;
    }
    private void apply(Element element,int x,int y,double scale){
        element.setter.set(x,y,scale);dirty=true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(dragging!=null){dragging=null;resizing=false;saveIfDirty();return true;}
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(dragging!=null)return true;
        var element=at(x,y);if(element==null)return super.mouseScrolled(x,y,horizontal,vertical);
        selected=element;var b=element.bounds();double old=element.scale.getAsDouble();
        double scale=HudAlignment.scrollScale(old,vertical);
        resize(element,scale,x-(x-b.x())/old*scale,y-(y-b.y())/old*scale);
        return true;
    }

    // Persist only after layout edits; simply opening/closing the editor should
    // not enqueue redundant configuration writes.
    private void saveIfDirty(){if(dirty){ConfigManager.save();dirty=false;}}
    @Override public void removed(){saveIfDirty();super.removed();}
    @Override public void onClose(){saveIfDirty();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}