package name.skyveil.client.mixin;

import name.skyveil.client.tooltip.ScrollableTooltipState;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method="removed",at=@At("HEAD"))
    private void skyveil$resetTooltipScroll(CallbackInfo ci){ScrollableTooltipState.reset();}
}
