package name.skyveil.client.itemprotection;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;

/** Edge-triggered world input for locking the selected hotbar item without opening inventory. */
public final class ItemProtectionInputHandler {
    private static boolean lockKeyWasDown;
    private ItemProtectionInputHandler(){}
    public static void tick(Minecraft client){
        if(!ConfigManager.get().itemProtection.enabled){lockKeyWasDown=false;return;}
        boolean down=InputConstants.isKeyDown(client.getWindow(),ConfigManager.get().itemProtection.lockKey);
        // Trigger once on the press edge, not every tick while the key is held.
        if(down&&!lockKeyWasDown&&client.screen==null&&client.player!=null){
            var selected=client.player.getInventory().getSelectedItem();
            if(!selected.isEmpty())ItemProtectionManager.toggle(client.player.getInventory().getSelectedSlot());
        }
        lockKeyWasDown=down;
    }
}
