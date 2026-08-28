package name.skyveil.client.chatcopy;

import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;

/** Display names, normalization, and matching for Chat Copy's single chord. */
public final class ChatCopyManager {
    private ChatCopyManager(){}
    public static boolean validMouse(int button){return button>=GLFW.GLFW_MOUSE_BUTTON_1&&button<=GLFW.GLFW_MOUSE_BUTTON_LAST;}
    public static boolean validKey(int key){return key==GLFW.GLFW_KEY_UNKNOWN||key>=GLFW.GLFW_KEY_SPACE&&key<=GLFW.GLFW_KEY_LAST;}
    public static String mouseName(int button){return switch(button){case GLFW.GLFW_MOUSE_BUTTON_LEFT->"Left Click";case GLFW.GLFW_MOUSE_BUTTON_RIGHT->"Right Click";case GLFW.GLFW_MOUSE_BUTTON_MIDDLE->"Middle Click";default->"Mouse "+(button+1);};}
    public static String keyName(int key){return switch(key){case GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL->"Ctrl";case GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT->"Shift";case GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT->"Alt";default->InputConstants.getKey(new KeyEvent(key,0,0)).getDisplayName().getString();};}
    public static String chordName(ChatCopyBinding binding){ArrayList<String> parts=new ArrayList<>();if(binding.keys!=null)for(Integer key:binding.keys)if(key!=null&&validKey(key)&&key!=GLFW.GLFW_KEY_UNKNOWN)parts.add(keyName(key));parts.add(mouseName(binding.mouseButton));return String.join(" + ",parts);}
    static boolean matches(ChatCopyBinding binding,int mouseButton,IntPredicate keyDown){if(binding==null||binding.mouseButton!=mouseButton)return false;if(binding.keys!=null)for(Integer key:binding.keys)if(key!=null&&!keyDown.test(key))return false;return true;}
    public static ChatCopyBinding normalize(ChatCopyBinding binding){if(binding==null)binding=new ChatCopyBinding();if(!validMouse(binding.mouseButton))binding.mouseButton=GLFW.GLFW_MOUSE_BUTTON_MIDDLE;if(binding.keys==null)binding.keys=new ArrayList<>();binding.keys=binding.keys.stream().filter(Objects::nonNull).filter(ChatCopyManager::validKey).filter(key->key!=GLFW.GLFW_KEY_UNKNOWN).distinct().sorted(java.util.Comparator.comparingInt(ChatCopyManager::displayOrder)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));return binding;}
    private static int displayOrder(int key){return switch(key){case GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL->0;case GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT->1;case GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT->2;default->10+key;};}
}
