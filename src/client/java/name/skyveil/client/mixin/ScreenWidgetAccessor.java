package name.skyveil.client.mixin;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses Screen's protected widget registration from inventory mixins. */
@Mixin(Screen.class)
public interface ScreenWidgetAccessor {
    // Use vanilla registration so the added widget participates in focus, narration,
    // input handling and rendering together.
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T skyveil$addRenderableWidget(T widget);
}
