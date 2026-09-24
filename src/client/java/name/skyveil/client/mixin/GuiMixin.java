package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
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
    @Inject(method="extractSelectedItemName",at=@At("HEAD"),cancellable=true)
    private void skyveil$selectedItemVisibility(CallbackInfo ci){
        if(SkyblockSession.isActive()&&!name.skyveil.client.config.ConfigManager.get().showSwitchedItemName)ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.ModifyArgs(
        method="extractSelectedItemName",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void skyveil$dimSelectedItem(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        if(!SkyblockSession.isActive()||!name.skyveil.client.gui.HudVisibility.dimmed())return;
        args.set(1,name.skyveil.client.gui.HudVisibility.dimText(args.get(1)));
        args.set(5,name.skyveil.client.gui.HudVisibility.color(args.get(5)));
    }

    @org.spongepowered.asm.mixin.injection.ModifyArgs(
        method="extractOverlayMessage",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void skyveil$dimSkillXp(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        if(!SkyblockSession.isActive()||!name.skyveil.client.gui.HudVisibility.dimmed())return;
        net.minecraft.network.chat.Component text=args.get(1);
        if(!name.skyveil.client.gui.HudVisibility.isSkillXp(text.getString()))return;
        args.set(1,name.skyveil.client.gui.HudVisibility.dimText(text));
        args.set(5,name.skyveil.client.gui.HudVisibility.color(args.get(5)));
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method="setOverlayMessage",at=@At("HEAD"),argsOnly=true)
    private net.minecraft.network.chat.Component skyveil$playerStats(net.minecraft.network.chat.Component message){
        return name.skyveil.client.stats.SkillXpHud.onActionBar(name.skyveil.client.stats.PlayerStatsHud.onActionBar(message));
    }

    @Inject(method="extractHearts",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideHearts(CallbackInfo ci){
        if(name.skyveil.client.stats.PlayerStatsHud.active())ci.cancel();
    }

    @Inject(method="extractFood",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideHunger(CallbackInfo ci){
        if(name.skyveil.client.stats.PlayerStatsHud.active())ci.cancel();
    }

    @Inject(method="extractSlot",at=@At("HEAD"))
    private void skyveil$drawHotbarRarity(GuiGraphicsExtractor graphics,int x,int y,DeltaTracker delta,Player player,ItemStack stack,int seed,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        ItemRarityRenderer.drawBelowItem(graphics,stack,x,y);
    }

    @Inject(method="extractSlot",at=@At("TAIL"))
    private void skyveil$drawHotbarLock(GuiGraphicsExtractor graphics,int x,int y,DeltaTracker delta,Player player,ItemStack stack,int seed,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        int slot=seed-1;
        if(slot>=0&&slot<9&&ItemProtectionManager.isLocked(slot))ItemLockOverlayRenderer.draw(graphics,x,y);
    }
}
