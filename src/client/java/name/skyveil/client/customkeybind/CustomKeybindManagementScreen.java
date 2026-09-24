package name.skyveil.client.customkeybind;

import name.skyveil.client.gui.SkyveilTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Scrollable management list for persistent custom command keybinds. */
public final class CustomKeybindManagementScreen extends Screen {
    private final Screen parent;
    private final List<RowAction> actions=new ArrayList<>();
    private double scroll;
    private String message="";
    private int left,top,panelWidth,panelHeight;

    public CustomKeybindManagementScreen(Screen parent){super(Component.literal("Custom Keybinds"));this.parent=parent;}
    public static void open(){Minecraft client=Minecraft.getInstance();client.setScreen(new CustomKeybindManagementScreen(client.screen));}

    @Override protected void init(){
        panelWidth=Math.min(540,width-28);panelHeight=Math.min(430,height-28);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        addRenderableWidget(new name.skyveil.client.gui.SkyveilButton(left+12,top+32,104,20,Component.literal("Add Keybind"),button->minecraft.setScreen(new CustomKeybindEditorScreen(this,null))));
        addRenderableWidget(new name.skyveil.client.gui.SkyveilButton(left+panelWidth-72,top+32,60,20,Component.literal("Done"),button->onClose()));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(0,0,width,height,SkyveilTheme.SCRIM);g.fill(left,top,left+panelWidth,top+panelHeight,SkyveilTheme.WINDOW);g.outline(left,top,panelWidth,panelHeight,SkyveilTheme.ACCENT);
        g.centeredText(font,title,width/2,top+10,SkyveilTheme.ACCENT);actions.clear();
        int viewTop=top+62,viewBottom=top+panelHeight-14,rowWidth=panelWidth-24;
        g.enableScissor(left+12,viewTop,left+panelWidth-12,viewBottom);try{
        List<CustomKeybindDefinition> bindings=CustomKeybindManager.all();int y=viewTop-(int)scroll;
        if(bindings.isEmpty())g.centeredText(font,"No custom keybinds configured.",width/2,y+18,SkyveilTheme.SECONDARY);
        for(CustomKeybindDefinition binding:bindings){
            boolean hover=inside(mx,my,left+12,y,rowWidth,48);g.fill(left+12,y,left+12+rowWidth,y+48,hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
            g.text(font,binding.name,left+20,y+7,SkyveilTheme.TEXT,true);g.text(font,binding.command,left+20,y+24,SkyveilTheme.SECONDARY,false);
            int deleteX=left+panelWidth-66,editX=deleteX-48,toggleX=editX-48,keyRight=toggleX-8;
            String key=CustomKeybindManager.keyName(binding.key);g.text(font,key,keyRight-font.width(key),y+19,SkyveilTheme.ACCENT,true);
            drawControl(g,toggleX,y+13,42,binding.enabled?"ON":"OFF",binding.enabled?SkyveilTheme.ACCENT:SkyveilTheme.OFF);
            drawControl(g,editX,y+13,42,"Edit",SkyveilTheme.ACCENT_DARK);drawControl(g,deleteX,y+13,48,"Delete",0xFF8E3D55);
            if(y+48>=viewTop&&y<viewBottom){actions.add(new RowAction(toggleX,y+13,42,22,Action.TOGGLE,binding));actions.add(new RowAction(editX,y+13,42,22,Action.EDIT,binding));actions.add(new RowAction(deleteX,y+13,48,22,Action.DELETE,binding));}y+=55;
        }
        }finally{g.disableScissor();}
        if(!message.isBlank())g.centeredText(font,message,width/2,top+panelHeight-11,0xFFFF7777);
        super.extractRenderState(g,mx,my,partial);
    }

    private void drawControl(GuiGraphicsExtractor g,int x,int y,int width,String label,int color){g.fill(x,y,x+width,y+22,color);g.centeredText(font,label,x+width/2,y+7,SkyveilTheme.TEXT);}

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()==0)for(RowAction row:actions)if(inside(event.x(),event.y(),row.x,row.y,row.width,row.height)){
            switch(row.action){
                case TOGGLE -> {if(!CustomKeybindManager.setEnabled(row.binding,!row.binding.enabled)){String conflict=CustomKeybindManager.customConflict(row.binding.key,row.binding.id);message="Key conflicts with \""+conflict+"\".";}else message="";}
                case EDIT -> minecraft.setScreen(new CustomKeybindEditorScreen(this,row.binding));
                case DELETE -> {CustomKeybindManager.delete(row.binding.id);message="";clampScroll();}
            }
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){
        if(inside(mouseX,mouseY,left+12,top+62,panelWidth-24,panelHeight-76)){scroll=Math.max(0,scroll-vertical*28);clampScroll();return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
    }
    private void clampScroll(){int content=CustomKeybindManager.all().size()*55,viewport=panelHeight-76;scroll=Math.min(scroll,Math.max(0,content-viewport));}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private enum Action{TOGGLE,EDIT,DELETE}
    private record RowAction(int x,int y,int width,int height,Action action,CustomKeybindDefinition binding){}
}
