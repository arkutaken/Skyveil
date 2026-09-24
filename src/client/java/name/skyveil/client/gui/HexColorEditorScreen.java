package name.skyveil.client.gui;

import name.skyveil.client.config.SettingDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Small focused editor for the hex-color plates shown in the settings workspace. */
public final class HexColorEditorScreen extends Screen {
    private final Screen parent;
    private final SettingDefinition setting;
    private EditBox value;
    private String error="";
    private int preview;

    public HexColorEditorScreen(Screen parent,SettingDefinition setting){
        super(Component.literal(setting.name));this.parent=parent;this.setting=setting;this.preview=(Integer)setting.getter.get();
    }

    @Override protected void init(){
        int left=width/2-130,top=height/2-65;
        value=new EditBox(font,left+14,top+42,170,22,Component.literal("Hex color"));
        value.setMaxLength(9);value.setValue(String.format(Locale.ROOT,"#%06X",preview&0xFFFFFF));
        value.setResponder(this::updatePreview);addRenderableWidget(value);
        addRenderableWidget(new SkyveilButton(left+14,top+86,108,22,Component.literal("Save"),button->save()));
        addRenderableWidget(new SkyveilButton(left+138,top+86,108,22,Component.literal("Cancel"),button->onClose()));
        setInitialFocus(value);
    }

    private void updatePreview(String input){Integer parsed=parse(input);if(parsed==null){error="Use six hex digits, for example #55FFFF";return;}preview=parsed;error="";}
    static Integer parse(String input){
        String text=input==null?"":input.trim();if(text.startsWith("#"))text=text.substring(1);if(text.length()!=6)return null;
        try{return 0xFF000000|Integer.parseInt(text,16);}catch(NumberFormatException ignored){return null;}
    }
    private void save(){Integer parsed=parse(value.getValue());if(parsed==null){updatePreview(value.getValue());return;}setting.set(parsed);minecraft.setScreen(parent);}

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        int left=width/2-130,top=height/2-65;g.fill(0,0,width,height,SkyveilTheme.SCRIM);g.fill(left,top,left+260,top+130,SkyveilTheme.WINDOW);g.outline(left,top,260,130,SkyveilTheme.ACCENT);
        g.centeredText(font,title,width/2,top+12,SkyveilTheme.ACCENT);g.text(font,"Hex color",left+14,top+31,SkyveilTheme.SECONDARY,false);g.fill(left+198,top+42,left+246,top+64,preview);g.outline(left+198,top+42,48,22,SkyveilTheme.TEXT);
        if(!error.isBlank())g.centeredText(font,error,width/2,top+72,0xFFFF7777);super.extractRenderState(g,mouseX,mouseY,partialTick);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
