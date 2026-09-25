package name.skyveil.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the mapped 26.1.2 container bounds to the final InventoryScreen layer hook. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int skyveil$getLeftPos();
    @Accessor("topPos") int skyveil$getTopPos();
    @Accessor("imageWidth") int skyveil$getImageWidth();
    @Accessor("imageHeight") int skyveil$getImageHeight();
    // Route remapped controls through the same slot-click path as vanilla input,
    // preserving container behavior and the protection injections on that method.
    @Invoker("slotClicked") void skyveil$clickSlot(Slot slot,int slotId,int button,ContainerInput input);
}
