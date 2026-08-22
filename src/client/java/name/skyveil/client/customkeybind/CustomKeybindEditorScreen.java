package name.skyveil.client.customkeybind;

import name.skyveil.client.gui.SkyveilTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Add/edit form with keyboard capture and conflict feedback. */
public final class CustomKeybindEditorScreen extends Screen {
    private final Screen parent;
    private final CustomKeybindDefinition existing;
    private final CustomKeybindDefinition draft;
    private EditBox nameBox,commandBox;
    private Button keyButton,enabledButton;
    private boolean capturing;
    private String error="";

    public CustomKeybindEditorScreen(Screen parent,CustomKeybindDefinition existing){
        super(Component.literal(existing==null?"Add Custom Keybind":"Edit Custom Keybind"));this.parent=parent;this.existing=existing;this.draft=existing==null?new CustomKeybindDefinition():existing.copy();
    }

    @Override protected void init(){
        int left=width/2-150,top=height/2-116;
        nameBox=new EditBox(font,left+12,top+38,276,20,Component.literal("Display name"));nameBox.setMaxLength(64);nameBox.setHint(Component.literal("Auction House"));nameBox.setValue(draft.name==null?"":draft.name);addRenderableWidget(nameBox);
        commandBox=new EditBox(font,left+12,top+78,276,20,Component.literal("Command"));commandBox.setMaxLength(256);commandBox.setHint(Component.literal("/ah"));commandBox.setValue(draft.command==null?"":draft.command);addRenderableWidget(commandBox);
        keyButton=addRenderableWidget(Button.builder(Component.empty(),button->{capturing=true;refreshButtons();}).bounds(left+12,top+118,134,20).build());
        enabledButton=addRenderableWidget(Button.builder(Component.empty(),button->{draft.enabled=!draft.enabled;refreshButtons();}).bounds(left+154,top+118,134,20).build());
        if(existing==null){
            addRenderableWidget(Button.builder(Component.literal("Save"),button->save()).bounds(left+12,top+174,134,20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"),button->onClose()).bounds(left+154,top+174,134,20).build());
        }else{
            addRenderableWidget(Button.builder(Component.literal("Save"),button->save()).bounds(left+12,top+174,86,20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"),button->onClose()).bounds(left+107,top+174,86,20).build());
            addRenderableWidget(Button.builder(Component.literal("Delete"),button->delete()).bounds(left+202,top+174,86,20).build());
        }
        refreshButtons();setInitialFocus(nameBox);
    }

    private void refreshButtons(){
        if(keyButton!=null)keyButton.setMessage(Component.literal(capturing?"Press a key...":"Key: "+CustomKeybindManager.keyName(draft.key)));
        if(enabledButton!=null)enabledButton.setMessage(Component.literal(draft.enabled?"Enabled: ON":"Enabled: OFF"));
    }

    private void captureFields(){draft.name=nameBox.getValue().trim();draft.command=CustomKeybindManager.normalizeCommand(commandBox.getValue());}
    private void save(){
        captureFields();
        if(draft.name.isBlank()){error="Enter a display name.";return;}
        if(draft.command.isBlank()){error="Enter a command.";return;}
        if(!CustomKeybindManager.validKey(draft.key)){error="Choose a keyboard key.";return;}
        String conflict=draft.enabled?CustomKeybindManager.customConflict(draft.key,existing==null?null:existing.id):null;
        if(conflict!=null){error="Key already assigned to \""+conflict+"\".";return;}
        if(existing!=null)draft.id=existing.id;CustomKeybindManager.save(draft);minecraft.setScreen(parent);
    }
    private void delete(){CustomKeybindManager.delete(existing.id);minecraft.setScreen(parent);}

    @Override public boolean keyPressed(KeyEvent event){
        if(capturing){
            if(event.key()==GLFW.GLFW_KEY_ESCAPE){capturing=false;refreshButtons();return true;}
            if(event.key()==GLFW.GLFW_KEY_BACKSPACE||event.key()==GLFW.GLFW_KEY_DELETE){draft.key=GLFW.GLFW_KEY_UNKNOWN;capturing=false;error="";refreshButtons();return true;}
            if(CustomKeybindManager.validKey(event.key())){draft.key=event.key();capturing=false;error="";refreshButtons();return true;}
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        int left=width/2-150,top=height/2-116;g.fill(0,0,width,height,SkyveilTheme.SCRIM);g.fill(left,top,left+300,top+232,SkyveilTheme.WINDOW);g.outline(left,top,300,232,SkyveilTheme.ACCENT);
        g.centeredText(font,title,width/2,top+10,SkyveilTheme.TEXT);g.text(font,"Name",left+12,top+27,SkyveilTheme.SECONDARY,false);g.text(font,"Command",left+12,top+67,SkyveilTheme.SECONDARY,false);g.text(font,"Keybind",left+12,top+107,SkyveilTheme.SECONDARY,false);
        String custom=CustomKeybindManager.customConflict(draft.key,existing==null?null:existing.id),other=CustomKeybindManager.otherConflict(draft.key);
        int infoY=top+145;if(custom!=null)g.centeredText(font,"Conflict: assigned to \""+custom+"\".",width/2,infoY,0xFFFF7777);else if(other!=null)g.centeredText(font,"Also bound to "+other+".",width/2,infoY,0xFFFFD166);
        if(!error.isBlank())g.centeredText(font,error,width/2,top+204,0xFFFF7777);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public void onClose(){if(capturing){capturing=false;refreshButtons();}else minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
