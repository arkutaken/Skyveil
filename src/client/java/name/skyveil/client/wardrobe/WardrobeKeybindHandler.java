package name.skyveil.client.wardrobe;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.mixin.ContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Converts one physical number-key press into one normal left-click on a resolved Wardrobe control. */
public final class WardrobeKeybindHandler {
    private static final Set<Integer> HELD_KEYS=new HashSet<>();
    private static String lastAction="none";
    private WardrobeKeybindHandler() {}

    public static boolean handle(AbstractContainerScreen<?> screen,KeyEvent event){
        int number=wardrobeNumber(event.key());
        if(number<1||!ConfigManager.get().wardrobe.numberKeys||!WardrobeMenuDetector.matches(screen))return false;
        if(screen.getFocused() instanceof EditBox field&&field.isFocused())return false;
        // Consume repeats without sending another click or allowing vanilla's
        // number-key hotbar swap to act on the hovered wardrobe item.
        if(!HELD_KEYS.add(event.key())){lastAction="key="+number+" detected=true repeated=true hoveredSlot=ignored vanillaInputCancelled=true clickSent=false";return true;}
        WardrobeSlotResolver.Resolution resolution=WardrobeSlotResolver.resolve(screen);
        WardrobeSlotResolver.ResolvedSlot resolved=resolution.slots().get(number);
        if(resolved==null){lastAction="key="+number+" detected=true requestedWardrobeSlot="+number+" resolvedContainerSlot=none hoveredSlot=ignored vanillaInputCancelled=true clickSent=false";return true;}
        var slot=screen.getMenu().getSlot(resolved.menuSlot());
        ((ContainerScreenAccessor)screen).skyveil$clickSlot(slot,resolved.menuSlot(),0,ContainerInput.PICKUP);
        lastAction="key="+number+" detected=true requestedWardrobeSlot="+number+" resolvedContainerSlot="+resolved.menuSlot()+" hoveredSlot=ignored vanillaInputCancelled=true clickSent=true";
        return true;
    }

    public static void tick(Minecraft client){
        if(!ConfigManager.get().wardrobe.numberKeys){HELD_KEYS.clear();WardrobeSlotResolver.clearUnless(null);return;}
        long window=client.getWindow().handle();
        HELD_KEYS.removeIf(key->GLFW.glfwGetKey(window,key)==GLFW.GLFW_RELEASE);
        WardrobeSlotResolver.clearUnless(client.screen instanceof AbstractContainerScreen<?> screen?screen:null);
    }

    public static List<String> debugLines(){
        Minecraft client=Minecraft.getInstance();
        if(!(client.screen instanceof AbstractContainerScreen<?> screen))return List.of("Wardrobe detected: no","Screen: not a container");
        WardrobeSlotResolver.Resolution resolution=WardrobeSlotResolver.resolve(screen);
        List<String> lines=new ArrayList<>();
        lines.add("Wardrobe detected: "+(WardrobeMenuDetector.matches(screen)?"yes":"no"));
        lines.add("Screen title: "+resolution.title());
        lines.add("Container slot count: "+resolution.containerSlotCount());
        lines.add("Page: "+(resolution.page()==null?"unknown":resolution.page().current()+"/"+resolution.page().total()));
        lines.add("Last key action: "+lastAction);
        if(resolution.slots().isEmpty())lines.add("Resolved wardrobe slots: none");
        else resolution.slots().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry->{
            var slot=entry.getValue();lines.add(entry.getKey()+" -> slot "+slot.menuSlot()+" ("+slot.itemName()+", number="+(slot.numberFromText()?"tooltip":"page layout")+")");
        });
        lines.addAll(resolution.diagnostics().stream().limit(12).toList());
        return lines;
    }

    private static int wardrobeNumber(int key){
        if(key==GLFW.GLFW_KEY_0||key==GLFW.GLFW_KEY_KP_0)return 10;
        if(key>=GLFW.GLFW_KEY_1&&key<=GLFW.GLFW_KEY_9)return key-GLFW.GLFW_KEY_0;
        if(key>=GLFW.GLFW_KEY_KP_1&&key<=GLFW.GLFW_KEY_KP_9)return key-GLFW.GLFW_KEY_KP_0;
        return -1;
    }
}
