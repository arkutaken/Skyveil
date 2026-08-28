package name.skyveil.client.mixin;

import name.skyveil.client.chatcopy.ChatCopyHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void skyveil$copyHoveredMessage(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){if(ChatCopyHandler.mouseClicked(event))cir.setReturnValue(true);}
}
