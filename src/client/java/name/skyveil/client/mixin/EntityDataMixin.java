package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
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
    // Spawn may precede the name metadata. Retry classification after the new
    // data is applied; the manager deduplicates already-accepted splashes.
    @Inject(method="onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",at=@At("TAIL"))
    private void skyveil$trackDamageMetadata(EntityDataAccessor<?> accessor,CallbackInfo ci){if(SkyblockSession.isActive())CompactDamageManager.onEntityAddedOrUpdated((Entity)(Object)this);}
}
