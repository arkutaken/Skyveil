package name.skyveil.client.chatcopy;

import com.mojang.blaze3d.platform.InputConstants;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.mixin.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Copies the complete parent message of the wrapped chat line under the cursor. */
public final class ChatCopyHandler {
    private ChatCopyHandler(){}
    public static boolean mouseClicked(MouseButtonEvent event){Minecraft client=Minecraft.getInstance();if(!ConfigManager.get().chatCopy.enabled||client.gui==null)return false;
        ChatCopyBinding binding=ChatCopyManager.normalize(ConfigManager.get().chatCopy.binding);if(!ChatCopyManager.matches(binding,event.button(),key->InputConstants.isKeyDown(client.getWindow(),key)))return false;
        Component message=hoveredMessage(client,event.x(),event.y());if(message==null)return false;String text=plainText(message.getString());if(text.isBlank())return false;client.keyboardHandler.setClipboard(text);client.gui.setOverlayMessage(Component.literal("Copied chat message"),false);return true;
    }
    static String plainText(String text){if(text==null||text.isEmpty())return "";StringBuilder plain=new StringBuilder(text.length());for(int index=0;index<text.length();index++){char current=text.charAt(index);if(current=='\u00c2'&&index+2<text.length()&&text.charAt(index+1)=='\u00a7'){index+=2;continue;}if(current=='\u00a7'&&index+1<text.length()){index++;continue;}plain.append(current);}return plain.toString();}
    static Component hoveredMessage(Minecraft client,double mouseX,double mouseY){ChatComponent chat=client.gui.getChat();ChatComponentAccessor access=(ChatComponentAccessor)chat;double scale=client.options.chatScale().get();if(scale<=0)return null;
        int x=Mth.floor(mouseX/scale),y=Mth.floor(mouseY/scale),bottom=Mth.floor((client.getWindow().getGuiScaledHeight()-40)/scale);int width=Mth.ceil(ChatComponent.getWidth(client.options.chatWidth().get())/scale)+4;if(x<0||x>width||y>bottom)return null;
        int lineHeight=(int)(9*(client.options.chatLineSpacing().get()+1));if(lineHeight<=0)return null;int visibleLine=(bottom-y)/lineHeight;if(visibleLine<0||visibleLine>=chat.getLinesPerPage())return null;int index=visibleLine+access.skyveil$getChatScrollbarPos();var lines=access.skyveil$getTrimmedMessages();if(index<0||index>=lines.size())return null;return lines.get(index).parent().content();
    }
}
