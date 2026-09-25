package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.tooltip.ScrollableTooltipState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Inject(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("TAIL"))
    private void skyveil$protectedItemMarker(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.level.Level level,ItemStack stack,int x,int y,int seed,CallbackInfo ci){
        if(!SkyblockSession.isActive()||!name.skyveil.client.itemprotection.ProtectedItemManager.protectedItem(stack))return;
        GuiGraphicsExtractor g=(GuiGraphicsExtractor)(Object)this;
        // Small cyan shield in the upper-right corner, clear of stack counts and durability.
        int sx=x+10,sy=y;
        g.fill(sx,sy,sx+6,sy+5,0xFF102A32);
        g.fill(sx+1,sy+5,sx+5,sy+6,0xFF102A32);
        g.fill(sx+2,sy+6,sx+4,sy+7,0xFF102A32);
        g.fill(sx+1,sy+1,sx+5,sy+4,0xFF55FFFF);
        g.fill(sx+2,sy+4,sx+4,sy+6,0xFF55FFFF);
    }

    // Tooltip background extraction exposes the final positioned bounds, including
    // scrolling; reserve that area before deferred container HUDs are submitted.
    @org.spongepowered.asm.mixin.injection.ModifyArgs(
        method="tooltip",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"))
    private void skyveil$keepTooltipAboveHud(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        if(!SkyblockSession.isActive()
            ||!(net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>))return;
        GuiGraphicsExtractor graphics=args.get(0);
        int x=args.get(1),y=args.get(2),width=args.get(3),height=args.get(4);
        name.skyveil.client.gui.HudVisibility.tooltip(graphics,x,y,width,height);
    }

    @Inject(method="setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",at=@At("HEAD"))
    private void skyveil$observeItemTooltip(Font font,ItemStack stack,int mouseX,int mouseY,CallbackInfo ci){if(SkyblockSession.isActive())ScrollableTooltipState.observe(stack);}
}
