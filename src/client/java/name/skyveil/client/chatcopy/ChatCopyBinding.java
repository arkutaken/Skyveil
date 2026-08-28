package name.skyveil.client.chatcopy;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** The single Gson-friendly chord used to copy the chat message under the cursor. */
public final class ChatCopyBinding {
    public int mouseButton=GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    public List<Integer> keys=new ArrayList<>();

    public ChatCopyBinding copy(){ChatCopyBinding copy=new ChatCopyBinding();copy.mouseButton=mouseButton;copy.keys=keys==null?new ArrayList<>():new ArrayList<>(keys);return copy;}
}
