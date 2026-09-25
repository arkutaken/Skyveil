package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.itemprotection.ItemProtectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels only the vanilla swap-hands action when either participating stack is protected. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {
    // Intercept only swap-hands packets. The offhand uses player inventory index
    // 40, which must be checked alongside the selected hotbar slot.
    @Inject(method="send",at=@At("HEAD"),cancellable=true)
    private void skyveil$preventLockedHandSwap(Packet<?> packet,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        if(!(packet instanceof ServerboundPlayerActionPacket action)||action.getAction()!=ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND)return;
        var player=Minecraft.getInstance().player;if(player==null)return;
        if(ItemProtectionManager.isProtected(player.getInventory().getSelectedSlot())||ItemProtectionManager.isProtected(40)){ItemProtectionManager.blocked();ci.cancel();}
    }
}
