package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.dungeon.DungeonItemTooltip;
import name.skyveil.client.tooltip.ScrollableTooltipState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    // Deferred HUD drawing waits until tooltips have supplied their final bounds.
    @Inject(method="extractRenderStateWithTooltipAndSubtitles",at=@At("RETURN"))
    private void skyveil$finishContainerHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        name.skyveil.client.gui.HudVisibility.finish(graphics);
    }

    @Inject(method="removed",at=@At("HEAD"))
    private void skyveil$resetTooltipScroll(CallbackInfo ci){ScrollableTooltipState.reset();}

    // Decorators consume the preceding result so enabled features compose rather
    // than overwrite one another's tooltip lines.
    @Inject(method="getTooltipFromItem",at=@At("RETURN"),cancellable=true)
    private static void skyveil$addDungeonItemInfo(Minecraft client,ItemStack stack,CallbackInfoReturnable<List<Component>> cir){
        if(SkyblockSession.isActive()&&ConfigManager.get().auctionTooltip)
            cir.setReturnValue(name.skyveil.client.auction.AuctionTooltip.decorate(stack,cir.getReturnValue()));
        if(SkyblockSession.isActive()&&ConfigManager.get().fullCraftCost)
            cir.setReturnValue(name.skyveil.client.craftcost.CraftCostTooltip.decorate(stack,cir.getReturnValue()));
        if(SkyblockSession.isActive()&&ConfigManager.get().bazaarTooltip)cir.setReturnValue(name.skyveil.client.bazaar.BazaarTooltip.decorate(stack,cir.getReturnValue()));
        if(SkyblockSession.isActive()&&ConfigManager.get().itemRarity.showDungeonFloorAndQuality)
            cir.setReturnValue(DungeonItemTooltip.decorate(stack,cir.getReturnValue()));
        if(SkyblockSession.isActive()&&name.skyveil.client.itemprotection.ProtectedItemManager.protectedItem(stack)){
            var lines=new java.util.ArrayList<>(cir.getReturnValue());
            lines.add(Component.literal("Skyveil: Drop Protected").withColor(0x55FFFF));cir.setReturnValue(lines);
        }
    }
}
