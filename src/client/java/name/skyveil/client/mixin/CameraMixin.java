package name.skyveil.client.mixin;

import name.skyveil.client.zoom.ZoomManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies zoom after vanilla has calculated effects such as sprinting, fluids, and death FOV. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void skyveil$zoomFov(float partialTick,CallbackInfoReturnable<Float> cir){cir.setReturnValue(ZoomManager.modifyFov(cir.getReturnValue()));}
}
