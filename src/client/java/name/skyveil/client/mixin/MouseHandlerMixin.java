package name.skyveil.client.mixin;

import name.skyveil.client.zoom.ZoomManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reserves the wheel for zoom strength only while the configured zoom key is held in gameplay. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void skyveil$adjustZoom(long window,double horizontal,double vertical,CallbackInfo ci){if(ZoomManager.onScroll(vertical))ci.cancel();}
}
