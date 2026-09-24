package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.itemprotection.ItemLockOverlayRenderer;
import name.skyveil.client.itemprotection.ItemProtectionManager;
import name.skyveil.client.itemprotection.ItemLinkOverlayRenderer;
import name.skyveil.client.itemrarity.ItemRarityRenderer;
import name.skyveil.client.itemsearch.ItemSearchOverlay;
import name.skyveil.client.inventorybuttons.InventoryButtonRenderer;
import name.skyveil.client.gui.ContainerDarkModeRenderer;
import name.skyveil.client.hunting.AttributeMenuPanel;
import name.skyveil.client.hunting.HuntingBoxValuePanel;
import name.skyveil.client.pet.PetTracker;
import name.skyveil.client.pet.PetMenuLevelOverlayRenderer;
import name.skyveil.client.pet.PetsMenuDetector;
import name.skyveil.client.wardrobe.WardrobeKeybindHandler;
import name.skyveil.client.equipment.EquipmentShortcutRow;
import name.skyveil.client.storage.StoragePreviewManager;
import name.skyveil.client.tooltip.ScrollableTooltipState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow protected abstract void slotClicked(Slot slot,int slotId,int button,ContainerInput input);
    @Inject(method="init",at=@At("TAIL"))
    private void skyveil$addItemSearch(CallbackInfo ci){if(!SkyblockSession.isActive())return;AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)(Object)this;if(StoragePreviewManager.isActive(screen)){topPos=Math.max(4,screen.height-imageHeight-89);ItemSearchOverlay.reset();return;}((ScreenWidgetAccessor)this).skyveil$addRenderableWidget(ItemSearchOverlay.create(screen,topPos,imageHeight));}

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void skyveil$drawInventoryButtons(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)(Object)this;
        boolean storagePreview=StoragePreviewManager.isActive(screen);
        if(!((Object)this instanceof InventoryScreen)&&!storagePreview)InventoryButtonRenderer.render((Screen)(Object)this,graphics,leftPos,topPos,imageWidth,imageHeight,mouseX,mouseY);
        AttributeMenuPanel.render(screen,graphics,leftPos,topPos,imageWidth,imageHeight,mouseX,mouseY);
        HuntingBoxValuePanel.render(screen,graphics,leftPos,topPos,imageWidth,imageHeight,mouseX,mouseY);
        EquipmentShortcutRow.observe(screen);
        if(!((Object)this instanceof InventoryScreen)&&!storagePreview)ItemSearchOverlay.render(screen,graphics,mouseX,mouseY);
    }

    @Inject(method="extractContents",at=@At("TAIL"))
    private void skyveil$drawStorageBeforeCarriedItem(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){if(SkyblockSession.isActive())StoragePreviewManager.observeAndRender((AbstractContainerScreen<?>)(Object)this,graphics,hoveredSlot,leftPos,topPos,imageWidth,imageHeight,mouseX,mouseY);}

    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void skyveil$clickInventoryButton(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)(Object)this;
        StoragePreviewManager.ClickResult storageClick=StoragePreviewManager.mouseClicked(screen,event,doubleClick);if(storageClick!=null){if(storageClick.slot()>=0&&storageClick.slot()<menu.slots.size()){Slot target=menu.slots.get(storageClick.slot());slotClicked(target,storageClick.slot(),storageClick.button(),storageClick.input());}cir.setReturnValue(true);return;}
        if(ItemSearchOverlay.blurOnOutsideClick(screen,event.x(),event.y())){cir.setReturnValue(true);return;}
        if(ItemSearchOverlay.mouseClicked(screen,event.x(),event.y(),event.button())){cir.setReturnValue(true);return;}
        if(AttributeMenuPanel.mouseClicked(screen,event.x(),event.y(),event.button())){cir.setReturnValue(true);return;}
        if(HuntingBoxValuePanel.mouseClicked(screen,event.x(),event.y())){cir.setReturnValue(true);return;}
        if(!((Object)this instanceof InventoryScreen)&&InventoryButtonRenderer.mouseClicked((Screen)(Object)this,leftPos,topPos,imageWidth,imageHeight,event))cir.setReturnValue(true);
    }

    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void skyveil$scrollOversizedTooltip(double mouseX,double mouseY,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        if(StoragePreviewManager.mouseScrolled((AbstractContainerScreen<?>)(Object)this,mouseX,mouseY,vertical)
            ||ItemSearchOverlay.mouseScrolled((AbstractContainerScreen<?>)(Object)this,mouseX,mouseY,vertical)
            ||AttributeMenuPanel.mouseScrolled((AbstractContainerScreen<?>)(Object)this,mouseX,mouseY,vertical)
            ||HuntingBoxValuePanel.mouseScrolled((AbstractContainerScreen<?>)(Object)this,mouseX,mouseY,vertical)
            ||ScrollableTooltipState.scroll((Screen)(Object)this,vertical))cir.setReturnValue(true);
    }

    @Inject(method="removed",at=@At("HEAD"))
    private void skyveil$resetTooltipScroll(CallbackInfo ci){AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)(Object)this;StoragePreviewManager.captureBeforeClose(screen);EquipmentShortcutRow.screenClosed(screen);ScrollableTooltipState.reset();AttributeMenuPanel.screenClosed(screen);HuntingBoxValuePanel.reset();ItemSearchOverlay.reset();ItemProtectionManager.clearPendingLink();name.skyveil.client.itemprotection.ProtectedItemManager.releaseKey();}

    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void skyveil$itemSearchKey(KeyEvent event,CallbackInfoReturnable<Boolean> cir){if(SkyblockSession.isActive()&&ItemSearchOverlay.keyPressed((AbstractContainerScreen<?>)(Object)this,event))cir.setReturnValue(true);}

    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void skyveil$toggleLock(KeyEvent event,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        if(ConfigManager.get().itemProtection.protectItems&&event.key()==ConfigManager.get().itemProtection.protectItemKey){
            if(hoveredSlot!=null)name.skyveil.client.itemprotection.ProtectedItemManager.press(hoveredSlot.getItem());
            cir.setReturnValue(true);return;
        }
        if(!ConfigManager.get().itemProtection.enabled||event.key()!=ConfigManager.get().itemProtection.lockKey)return;
        ItemProtectionManager.beginLockKeyPress(hoveredSlot);
        cir.setReturnValue(true);
    }

    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void skyveil$wardrobeNumberKey(KeyEvent event,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        if(WardrobeKeybindHandler.handle((AbstractContainerScreen<?>)(Object)this,event))cir.setReturnValue(true);
    }

    @Inject(method="onClose",at=@At("HEAD"),cancellable=true)
    private void skyveil$protectCarriedOnClose(CallbackInfo ci){
        if(SkyblockSession.isActive()&&name.skyveil.client.itemprotection.ProtectedItemManager.protectedItem(menu.getCarried())){
            name.skyveil.client.itemprotection.ProtectedItemManager.blocked();ci.cancel();
        }
    }

    @Inject(method="slotClicked",at=@At("HEAD"),cancellable=true)
    private void skyveil$protectSlot(Slot slot,int slotId,int button,ContainerInput input,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        if(ConfigManager.get().itemProtection.enabled&&(Object)this instanceof InventoryScreen&&ItemProtectionManager.lockKeyHeld()
            &&button==0&&(input==ContainerInput.PICKUP||input==ContainerInput.QUICK_MOVE)&&ItemProtectionManager.handleLinkClick(slot)){ci.cancel();return;}
        if(ConfigManager.get().itemProtection.enabled&&(Object)this instanceof InventoryScreen&&input==ContainerInput.QUICK_MOVE
            &&ItemProtectionManager.redirectLinkedQuickMove(menu,slot)){ci.cancel();return;}
        var player=Minecraft.getInstance().player;
        if(slot!=null&&(player==null||slot.container!=player.getInventory()))PetTracker.onPetsMenuInteraction((AbstractContainerScreen<?>)(Object)this,slot);
        if(ConfigManager.get().itemProtection.enabled){
            boolean blocked=ItemProtectionManager.isLocked(slot);
            if(input==ContainerInput.SWAP&&Minecraft.getInstance().player!=null&&(button>=0&&button<9||button==40))blocked|=ItemProtectionManager.isLocked(button);
            if(input==ContainerInput.PICKUP_ALL){
                ItemStack collected=!menu.getCarried().isEmpty()?menu.getCarried():slot==null?ItemStack.EMPTY:slot.getItem();
                if(!collected.isEmpty())for(Slot candidate:menu.slots)if(ItemStack.isSameItemSameComponents(collected,candidate.getItem()))blocked|=ItemProtectionManager.isLocked(candidate);
            }
            if(blocked){ItemProtectionManager.blocked();ci.cancel();return;}
        }
        if(input==ContainerInput.QUICK_MOVE){
            var targets=StoragePreviewManager.stackMergeTargets((AbstractContainerScreen<?>)(Object)this,slot);if(!targets.isEmpty()){
                slotClicked(slot,slotId,0,ContainerInput.PICKUP);for(int targetId:targets){if(menu.getCarried().isEmpty())break;Slot target=menu.slots.get(targetId);slotClicked(target,targetId,0,ContainerInput.PICKUP);}if(!menu.getCarried().isEmpty())slotClicked(slot,slotId,0,ContainerInput.PICKUP);Minecraft client=Minecraft.getInstance();if(slot.hasItem()&&client.gameMode!=null&&client.player!=null)client.gameMode.handleContainerInput(menu.containerId,slotId,0,ContainerInput.QUICK_MOVE,client.player);ci.cancel();
            }
        }
    }

    @Inject(method="extractContents",at=@At("HEAD"))
    private void skyveil$darkenContainerBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        if(!((Object)this instanceof InventoryScreen)&&!StoragePreviewManager.isActive((AbstractContainerScreen<?>)(Object)this))ContainerDarkModeRenderer.render(graphics,leftPos,topPos,imageWidth,imageHeight);
    }

    @Inject(method="extractLabels",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideStorageLabels(GuiGraphicsExtractor graphics,int mouseX,int mouseY,CallbackInfo ci){if(SkyblockSession.isActive()&&StoragePreviewManager.isActive((AbstractContainerScreen<?>)(Object)this))ci.cancel();}

    @ModifyArg(method="extractLabels",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),index=4)
    private int skyveil$darkModeLabelColor(int vanilla){return ContainerDarkModeRenderer.labelColor(vanilla);}

    @Inject(method="extractContents",at=@At("TAIL"))
    private void skyveil$drawItemLinks(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        if((Object)this instanceof InventoryScreen)ItemLinkOverlayRenderer.drawConnection(graphics,menu,hoveredSlot,leftPos,topPos);
    }

    @Inject(method="extractTooltip",at=@At("HEAD"),cancellable=true)
    private void skyveil$observeScrollableTooltip(GuiGraphicsExtractor graphics,int mouseX,int mouseY,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        Minecraft client=Minecraft.getInstance();if(StoragePreviewManager.isActive((AbstractContainerScreen<?>)(Object)this)&&hoveredSlot!=null&&client.player!=null&&hoveredSlot.container!=client.player.getInventory()){ci.cancel();return;}
        ItemStack displayed=hoveredSlot==null?null:hoveredSlot.getItem();
        ScrollableTooltipState.observe(displayed,(Screen)(Object)this,hoveredSlot==null?-1:hoveredSlot.index,menu.containerId);
    }

    @Inject(method="extractSlot",at=@At("TAIL"))
    private void skyveil$drawLock(GuiGraphicsExtractor graphics,Slot slot,int x,int y,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        // AbstractContainerScreen has already translated the pose to leftPos/topPos here.
        PetMenuLevelOverlayRenderer.draw((AbstractContainerScreen<?>)(Object)this,graphics,slot);
        if(ItemProtectionManager.isLocked(slot))ItemLockOverlayRenderer.draw(graphics,slot.x,slot.y);
        if((Object)this instanceof InventoryScreen)ItemLinkOverlayRenderer.drawSlot(graphics,slot);
    }

    @Inject(method="extractSlot",at=@At("HEAD"),cancellable=true)
    private void skyveil$drawRarityBelowItem(GuiGraphicsExtractor graphics,Slot slot,int x,int y,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        Minecraft client=Minecraft.getInstance();if(StoragePreviewManager.isActive((AbstractContainerScreen<?>)(Object)this)&&client.player!=null&&slot.container!=client.player.getInventory()){ci.cancel();return;}
        if((Object)this instanceof InventoryScreen&&EquipmentShortcutRow.isOffhandSlot(slot)){ci.cancel();return;}
        // The screen pose is already translated to leftPos/topPos. Vanilla extracts the
        // item after this point, and the lock injection above remains the final layer.
        if(slot!=null){
            AbstractContainerScreen<?> screen=(AbstractContainerScreen<?>)(Object)this;
            ItemRarityRenderer.drawBelowItem(graphics,slot.getItem(),slot.x,slot.y,PetsMenuDetector.matches(screen)?PetTracker.displayedMenuRarity(slot.getItem()):null);
            ItemSearchOverlay.drawLoreMatch(graphics,slot.getItem(),slot.x,slot.y);
        }
    }

    @Inject(method="isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideOffhandHover(Slot slot,double mouseX,double mouseY,CallbackInfoReturnable<Boolean> cir){
        if(!SkyblockSession.isActive())return;
        Boolean remapped=StoragePreviewManager.remappedHover((AbstractContainerScreen<?>)(Object)this,slot,mouseX,mouseY);if(remapped!=null){cir.setReturnValue(remapped);return;}if((Object)this instanceof InventoryScreen&&EquipmentShortcutRow.isOffhandSlot(slot))cir.setReturnValue(false);
    }

    @Inject(method="hasClickedOutside",at=@At("HEAD"),cancellable=true)
    private void skyveil$keepRemappedStorageClicksInside(double mouseX,double mouseY,int guiLeft,int guiTop,CallbackInfoReturnable<Boolean> cir){if(SkyblockSession.isActive()&&StoragePreviewManager.isOverLiveStorage((AbstractContainerScreen<?>)(Object)this,mouseX,mouseY))cir.setReturnValue(false);}

    @Inject(method="extractSlotHighlightBack",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideOriginalStorageHighlightBack(GuiGraphicsExtractor graphics,CallbackInfo ci){if(skyveil$hoveringRemappedStorage())ci.cancel();}

    @Inject(method="extractSlotHighlightFront",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideOriginalStorageHighlightFront(GuiGraphicsExtractor graphics,CallbackInfo ci){if(skyveil$hoveringRemappedStorage())ci.cancel();}

    private boolean skyveil$hoveringRemappedStorage(){Minecraft client=Minecraft.getInstance();return SkyblockSession.isActive()&&StoragePreviewManager.isActive((AbstractContainerScreen<?>)(Object)this)&&hoveredSlot!=null&&client.player!=null&&hoveredSlot.container!=client.player.getInventory();}

}
