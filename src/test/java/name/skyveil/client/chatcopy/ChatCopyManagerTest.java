package name.skyveil.client.chatcopy;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class ChatCopyManagerTest {
    @Test void middleClickBindingNeedsNoKeyboardKey(){ChatCopyBinding binding=new ChatCopyBinding();assertTrue(ChatCopyManager.matches(binding,GLFW.GLFW_MOUSE_BUTTON_MIDDLE,key->false));assertFalse(ChatCopyManager.matches(binding,GLFW.GLFW_MOUSE_BUTTON_LEFT,key->false));}
    @Test void controlCLeftClickRequiresEveryRecordedInput(){ChatCopyBinding binding=new ChatCopyBinding();binding.mouseButton=GLFW.GLFW_MOUSE_BUTTON_LEFT;binding.keys=java.util.List.of(GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_C);assertTrue(ChatCopyManager.matches(binding,GLFW.GLFW_MOUSE_BUTTON_LEFT,key->key==GLFW.GLFW_KEY_LEFT_CONTROL||key==GLFW.GLFW_KEY_C));assertFalse(ChatCopyManager.matches(binding,GLFW.GLFW_MOUSE_BUTTON_LEFT,key->key==GLFW.GLFW_KEY_C));assertFalse(ChatCopyManager.matches(binding,GLFW.GLFW_MOUSE_BUTTON_LEFT,key->key==GLFW.GLFW_KEY_LEFT_CONTROL));}
    @Test void clipboardTextNeverContainsMinecraftFormattingCodes(){assertEquals("Player: Hello!",ChatCopyHandler.plainText("§6Player§r: §aHello!"));assertEquals("Symbols ✦ remain",ChatCopyHandler.plainText("Symbols ✦ remain"));}
}
