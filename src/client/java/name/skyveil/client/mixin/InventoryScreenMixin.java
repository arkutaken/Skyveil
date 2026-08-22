package name.skyveil.client.mixin;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.inventorybuttons.InventoryButtonRenderer;
import name.skyveil.client.gui.ContainerDarkModeRenderer;
import name.skyveil.client.itemsearch.ItemSearchOverlay;
import name.skyveil.client.equipment.EquipmentShortcutRow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after all 26.1.2 inventory recipe/effect layers and before they consume custom clicks. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void skyveil$conditionallyDrawPotionEffects(EffectsInInventory effects,GuiGraphicsExtractor graphics,int mouseX,int mouseY){
        if(ConfigManager.get().inventoryButtons.showPotionEffects)effects.extractRenderState(graphics,mouseX,mouseY);
    }

    @Inject(method="extractBackground",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",shift=At.Shift.BEFORE))
    private void skyveil$darkenBeforePlayerModel(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        ContainerScreenAccessor screen=(ContainerScreenAccessor)this;
        ContainerDarkModeRenderer.render(graphics,screen.skyveil$getLeftPos(),screen.skyveil$getTopPos(),screen.skyveil$getImageWidth(),screen.skyveil$getImageHeight());
    }

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void skyveil$drawInventoryButtonsLast(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        ContainerScreenAccessor screen=(ContainerScreenAccessor)this;
        EquipmentShortcutRow.render(graphics,screen.skyveil$getLeftPos(),screen.skyveil$getTopPos(),mouseX,mouseY);
        InventoryButtonRenderer.render((Screen)(Object)this,graphics,screen.skyveil$getLeftPos(),screen.skyveil$getTopPos(),screen.skyveil$getImageWidth(),screen.skyveil$getImageHeight(),mouseX,mouseY);
        ItemSearchOverlay.render((InventoryScreen)(Object)this,graphics,mouseX,mouseY);
    }
}
