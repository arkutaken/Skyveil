package name.skyveil.client.inventorybuttons;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.ContainerDarkModeRenderer;
import name.skyveil.client.storage.StoragePreviewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/** Draws and handles only the controls outside the vanilla container bounds. */
public final class InventoryButtonRenderer {
    private InventoryButtonRenderer() {}
    public static boolean applies(Screen screen){
        if(screen instanceof AbstractContainerScreen<?> container&&StoragePreviewManager.isActive(container))return false;
        return InventoryButtonManager.isManagement(screen)
            || ConfigManager.get().inventoryButtons.enabled&&(screen instanceof InventoryScreen||ConfigManager.get().inventoryButtons.showInContainers);
    }

    public static void render(Screen screen,GuiGraphicsExtractor graphics,int guiLeft,int guiTop,int guiWidth,int guiHeight,int mouseX,int mouseY){
        var config=ConfigManager.get().inventoryButtons;if(!applies(screen))return;var palette=ContainerDarkModeRenderer.controlPalette();
        var layout=InventoryButtonManager.layout();
        for(var position:InventoryButtonPosition.values()){
            var rectangle=position.rectangle(guiLeft,guiTop,guiWidth,guiHeight,graphics.guiWidth(),graphics.guiHeight(),config.scale);if(rectangle.isEmpty())continue;
            var rect=rectangle.get();InventoryButtonDefinition button=layout.get(position);if(button==null&&!InventoryButtonManager.isManagement(screen))continue;
            boolean hover=rect.contains(mouseX,mouseY);int background=hover?palette.hover():palette.panel();
            graphics.fill(rect.x(),rect.y(),rect.x()+rect.size(),rect.y()+rect.size(),background);graphics.outline(rect.x(),rect.y(),rect.size(),rect.size(),hover?palette.accent():palette.outline());
            if(button==null)graphics.centeredText(Minecraft.getInstance().font,"+",rect.x()+rect.size()/2,rect.y()+(rect.size()-8)/2,palette.accent());
            else drawIcon(graphics,InventoryButtonManager.icon(button.icon),rect);
            if(hover&&config.showTooltips){
                if(button==null)graphics.setTooltipForNextFrame(Component.literal("Add Inventory Button"),mouseX,mouseY);
                else{var lines=new ArrayList<Component>();lines.add(Component.literal("/"+button.command));lines.add(Component.literal("Left Click: Run"));lines.add(Component.literal("Right Click: Edit"));graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,lines,mouseX,mouseY);}
            }
        }
    }

    public static boolean mouseClicked(Screen screen,int guiLeft,int guiTop,int guiWidth,int guiHeight,MouseButtonEvent event){
        var config=ConfigManager.get().inventoryButtons;if(!applies(screen)||event.button()!=0&&event.button()!=1)return false;
        Minecraft client=Minecraft.getInstance();int screenWidth=client.getWindow().getGuiScaledWidth(),screenHeight=client.getWindow().getGuiScaledHeight();
        for(var position:InventoryButtonPosition.values()){
            var rectangle=position.rectangle(guiLeft,guiTop,guiWidth,guiHeight,screenWidth,screenHeight,config.scale);if(rectangle.isEmpty()||!rectangle.get().contains(event.x(),event.y()))continue;
            InventoryButtonDefinition button=InventoryButtonManager.at(position);
            if(button==null){if(InventoryButtonManager.isManagement(screen)&&event.button()==0)client.setScreen(new InventoryButtonEditorScreen(screen,null,position));else return false;}
            else if(event.button()==0)InventoryButtonManager.execute(button);else client.setScreen(new InventoryButtonEditorScreen(screen,button,position));
            return true;
        }
        return false;
    }

    private static void drawIcon(GuiGraphicsExtractor graphics,net.minecraft.world.item.ItemStack stack,InventoryButtonPosition.Rect rect){
        float scale=Math.min(1.25f,rect.size()/20.0f);int rendered=Math.round(16*scale),x=rect.x()+(rect.size()-rendered)/2,y=rect.y()+(rect.size()-rendered)/2;
        graphics.pose().pushMatrix();graphics.pose().scale(scale,scale);graphics.item(stack,Math.round(x/scale),Math.round(y/scale));graphics.pose().popMatrix();
    }
}
