package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.storage.StoragePreviewManager;
import name.skyveil.client.gui.ContainerDarkModeRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the duplicate vanilla chest window while retaining its real server slots. */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Shadow @Final private static Identifier CONTAINER_BACKGROUND;
    @Shadow @Final private int containerRows;

    @Inject(method="extractBackground",at=@At("HEAD"),cancellable=true)
    private void skyveil$hideStorageContainer(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
        ContainerScreen screen=(ContainerScreen)(Object)this;if(!SkyblockSession.isActive()||!StoragePreviewManager.isActive(screen))return;ContainerScreenAccessor bounds=(ContainerScreenAccessor)this;int left=bounds.skyveil$getLeftPos(),inventoryY=bounds.skyveil$getTopPos()+containerRows*18+17,width=bounds.skyveil$getImageWidth();graphics.blit(RenderPipelines.GUI_TEXTURED,CONTAINER_BACKGROUND,left,inventoryY,0,126,width,96,256,256);ContainerDarkModeRenderer.render(graphics,left,inventoryY,width,96);ci.cancel();
    }
}
