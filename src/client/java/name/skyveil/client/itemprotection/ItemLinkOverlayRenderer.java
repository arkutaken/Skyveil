package name.skyveil.client.itemprotection;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Draws persistent slot-link outlines and one lightweight hover connection. */
public final class ItemLinkOverlayRenderer {
    private static final int OUTLINE=0xFF55E6FF,LINE=0xCC55E6FF,PENDING=0xFFFFD75A;
    private ItemLinkOverlayRenderer(){}

    public static void drawSlot(GuiGraphicsExtractor graphics,Slot slot){
        if(!ConfigManager.get().itemProtection.enabled)return;
        int index=ItemProtectionManager.playerInventoryIndex(slot);if(index<0)return;
        if(ItemProtectionManager.isLinked(index)||ItemProtectionManager.isPending(index))
            graphics.outline(slot.x-1,slot.y-1,18,18,ItemProtectionManager.isPending(index)?PENDING:OUTLINE);
    }

    public static void drawConnection(GuiGraphicsExtractor graphics,AbstractContainerMenu menu,Slot hovered,int left,int top){
        if(!ConfigManager.get().itemProtection.enabled)return;
        int hoveredIndex=ItemProtectionManager.playerInventoryIndex(hovered),hotbar=ItemProtectionManager.linkedHotbar(hoveredIndex);
        if(hotbar<0)return;Slot hotbarSlot=find(menu,hotbar);if(hotbarSlot==null)return;
        for(Integer inventory:ItemProtectionManager.linkedInventorySlots(hotbar)){
            Slot destination=find(menu,inventory);if(destination!=null)drawLine(graphics,left+hotbarSlot.x+8,top+hotbarSlot.y+8,left+destination.x+8,top+destination.y+8);
        }
    }

    private static Slot find(AbstractContainerMenu menu,int inventoryIndex){
        for(Slot candidate:menu.slots)if(ItemProtectionManager.playerInventoryIndex(candidate)==inventoryIndex)return candidate;return null;
    }

    private static void drawLine(GuiGraphicsExtractor graphics,int x0,int y0,int x1,int y1){
        int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,error=dx+dy;
        while(true){graphics.fill(x0,y0,x0+1,y0+1,LINE);if(x0==x1&&y0==y1)break;int twice=2*error;if(twice>=dy){error+=dy;x0+=sx;}if(twice<=dx){error+=dx;y0+=sy;}}
    }
}
