package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.tooltip.ScrollableTooltipState;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultTooltipPositioner.class)
public abstract class DefaultTooltipPositionerMixin {
    @Inject(method="positionTooltip",at=@At("RETURN"),cancellable=true)
    private void skyveil$applyVerticalScroll(int screenWidth,int screenHeight,int mouseX,int mouseY,int tooltipWidth,int tooltipHeight,CallbackInfoReturnable<Vector2ic> cir){
        if(SkyblockSession.isActive())cir.setReturnValue(ScrollableTooltipState.position(screenHeight,tooltipHeight,cir.getReturnValue()));
    }
}
