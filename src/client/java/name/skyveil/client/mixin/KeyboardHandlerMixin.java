package name.skyveil.client.mixin;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.itemprotection.ItemProtectionManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Completes deferred lock/link handling at Minecraft's actual 26.1.2 keyboard dispatcher. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method="keyPress",at=@At("HEAD"))
    private void skyveil$releaseItemProtectionKey(long window,int action,KeyEvent event,CallbackInfo ci){
        if(action==GLFW.GLFW_RELEASE&&event.key()==ConfigManager.get().itemProtection.protectItemKey)name.skyveil.client.itemprotection.ProtectedItemManager.releaseKey();
        if(action==GLFW.GLFW_RELEASE&&event.key()==ConfigManager.get().itemProtection.lockKey){
            if(name.skyveil.client.SkyblockSession.isActive())ItemProtectionManager.endLockKeyPress();else ItemProtectionManager.clearPendingLink();
        }
    }
}
