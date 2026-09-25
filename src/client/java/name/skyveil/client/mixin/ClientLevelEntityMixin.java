package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.combat.CompactDamageManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes client-loaded entity lifecycle events for compact damage labels. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelEntityMixin {
    // Run after insertion so the manager can resolve nearby entities in the world.
    @Inject(method="addEntity",at=@At("TAIL"))
    private void skyveil$trackDamageEntity(Entity entity,CallbackInfo ci){if(SkyblockSession.isActive())CompactDamageManager.onEntityAddedOrUpdated(entity);}

    // Drop cached IDs before an entity disappears or its numeric ID is reused.
    @Inject(method="removeEntity",at=@At("HEAD"))
    private void skyveil$removeDamageEntity(int entityId,Entity.RemovalReason reason,CallbackInfo ci){if(SkyblockSession.isActive())CompactDamageManager.onEntityRemoved(entityId);}
}
