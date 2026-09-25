package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.inventorybuttons.InventoryButtonRenderer;
import name.skyveil.client.equipment.EquipmentShortcutRow;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives surrounding inventory controls priority over the recipe-book click handler. */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    // The subclass recipe-book handler can consume clicks before the base container
    // hook sees them, so surrounding inventory controls are checked here too.
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void skyveil$clickInventoryButtonFirst(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        if(!((Object)this instanceof InventoryScreen))return;
        ContainerScreenAccessor screen=(ContainerScreenAccessor)this;
        if(EquipmentShortcutRow.mouseClicked(screen.skyveil$getLeftPos(),screen.skyveil$getTopPos(),event.x(),event.y(),event.button())){cir.setReturnValue(true);return;}
        if(InventoryButtonRenderer.mouseClicked((Screen)(Object)this,screen.skyveil$getLeftPos(),screen.skyveil$getTopPos(),screen.skyveil$getImageWidth(),screen.skyveil$getImageHeight(),event))cir.setReturnValue(true);
    }
}
