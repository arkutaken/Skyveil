package name.skyveil.client.mixin;

import name.skyveil.client.itemprotection.ItemProtectionManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method="drop",at=@At("HEAD"),cancellable=true)
    private void skyveil$preventLockedDrop(boolean fullStack,CallbackInfoReturnable<Boolean> cir){
        LocalPlayer player=(LocalPlayer)(Object)this;
        if(ItemProtectionManager.isProtected(player.getInventory().getSelectedSlot())){ItemProtectionManager.blocked();cir.setReturnValue(false);}
    }
}
