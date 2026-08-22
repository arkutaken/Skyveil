package name.skyveil.client.mixin;

import name.skyveil.client.itemprotection.ItemLockOverlayRenderer;
import name.skyveil.client.itemprotection.ItemProtectionManager;
import name.skyveil.client.itemrarity.ItemRarityRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the same lock background before vanilla renders each hotbar item. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method="extractSlot",at=@At("HEAD"))
    private void skyveil$drawHotbarRarity(GuiGraphicsExtractor graphics,int x,int y,DeltaTracker delta,Player player,ItemStack stack,int seed,CallbackInfo ci){
        ItemRarityRenderer.drawBelowItem(graphics,stack,x,y);
    }

    @Inject(method="extractSlot",at=@At("TAIL"))
    private void skyveil$drawHotbarLock(GuiGraphicsExtractor graphics,int x,int y,DeltaTracker delta,Player player,ItemStack stack,int seed,CallbackInfo ci){
        int slot=seed-1;
        if(slot>=0&&slot<9&&ItemProtectionManager.isLocked(slot))ItemLockOverlayRenderer.draw(graphics,x,y);
    }
}
