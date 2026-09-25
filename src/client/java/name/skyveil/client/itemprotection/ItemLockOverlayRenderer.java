package name.skyveil.client.itemprotection;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Primitive lock mark: no font or emoji dependency, and its size is isolated here. */
public final class ItemLockOverlayRenderer {
    public static final int ICON_SIZE=14;
    private ItemLockOverlayRenderer(){}
    public static void draw(GuiGraphicsExtractor graphics,int slotX,int slotY){
        var config=ConfigManager.get().itemProtection;if(!config.enabled||!config.showLockIcon)return;
        // Apply user opacity to primitive colors; the lock has no texture/font dependency.
        int alpha=(int)Math.round(config.lockIconOpacity*140.0);int yellow=(alpha<<24)|0xFFD21F,darkYellow=(alpha<<24)|0xB8860B;
        int x=slotX+1,y=slotY+1;
        graphics.fill(x+4,y,x+10,y+2,yellow);graphics.fill(x+2,y+1,x+5,y+7,yellow);graphics.fill(x+9,y+1,x+12,y+7,yellow);
        graphics.fill(x,y+5,x+14,y+14,darkYellow);graphics.fill(x+1,y+6,x+13,y+13,yellow);
        graphics.fill(x+6,y+8,x+8,y+12,darkYellow);
    }
}
