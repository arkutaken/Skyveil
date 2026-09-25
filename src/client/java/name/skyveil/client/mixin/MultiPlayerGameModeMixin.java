package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.combat.CompactDamageManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks real player attack actions so secondary damage ticks cannot consume melee hits. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    // Check the outgoing action against the current menu. Outside-drop clicks
    // refer to the carried stack, while regular drops refer to a menu slot.
    @Inject(method="handleContainerInput",at=@At("HEAD"),cancellable=true)
    private void skyveil$protectItemDrop(int containerId,int slotId,int button,net.minecraft.world.inventory.ContainerInput input,Player player,CallbackInfo ci){
        if(!SkyblockSession.isActive()||!name.skyveil.client.itemprotection.ProtectedItemManager.drops(input,slotId))return;
        var menu=player.containerMenu;
        if(menu.containerId!=containerId)return;
        var stack=slotId==-999?menu.getCarried():slotId>=0&&slotId<menu.slots.size()?menu.slots.get(slotId).getItem():net.minecraft.world.item.ItemStack.EMPTY;
        if(name.skyveil.client.itemprotection.ProtectedItemManager.protectedItem(stack)){
            name.skyveil.client.itemprotection.ProtectedItemManager.blocked();ci.cancel();
        }
    }

    @Inject(method="attack",at=@At("HEAD"))
    private void skyveil$recordMeleeAttack(Player player,Entity target,CallbackInfo ci){if(SkyblockSession.isActive())CompactDamageManager.onMeleeAttack(target);}
}
