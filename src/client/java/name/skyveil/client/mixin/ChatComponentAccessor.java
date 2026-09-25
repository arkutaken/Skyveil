package name.skyveil.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    // Wrapped visible rows retain their parent message, letting Chat Copy recover
    // the full text while accounting for the current scrollbar position.
    @Accessor("trimmedMessages") List<GuiMessage.Line> skyveil$getTrimmedMessages();
    @Accessor("chatScrollbarPos") int skyveil$getChatScrollbarPos();
}
