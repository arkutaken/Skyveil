package name.skyveil.client.mixin;

import name.skyveil.client.combat.CompactDamageManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-evaluates Armor Stands when delayed custom-name metadata arrives. */
@Mixin(Entity.class)
public abstract class EntityDataMixin {
    @Inject(method="onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",at=@At("TAIL"))
    private void skyveil$trackDamageMetadata(EntityDataAccessor<?> accessor,CallbackInfo ci){CompactDamageManager.onEntityAddedOrUpdated((Entity)(Object)this);}
}
