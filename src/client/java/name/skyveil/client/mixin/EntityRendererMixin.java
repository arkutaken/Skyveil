package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.combat.CompactDamageManager;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Alters only the extracted name-tag render state; server entities and their metadata remain intact. */
@Mixin(ArmorStandRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V",at=@At("TAIL"))
    private void skyveil$compactDamageLabel(ArmorStand stand,ArmorStandRenderState state,float partialTick,CallbackInfo ci){if(SkyblockSession.isActive()&&CompactDamageManager.shouldSuppress(stand))state.nameTag=null;}
}
