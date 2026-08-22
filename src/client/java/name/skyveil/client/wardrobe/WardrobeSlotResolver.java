package name.skyveil.client.wardrobe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves key numbers to the verified Wardrobe selector row, independent of mouse hover. */
public final class WardrobeSlotResolver {
    private static final Pattern SLOT_NUMBER=Pattern.compile("(?i)\\b(?:wardrobe\\s+)?slot\\s*#?\\s*(\\d{1,2})\\b");
    private static final Pattern REVERSED_SLOT_NUMBER=Pattern.compile("(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+wardrobe\\s+slot\\b");
    private static final Pattern LOCKED=Pattern.compile("(?i)(?:\\blocked\\b|\\bunavailable\\b|not\\s+unlocked|unlock\\s+this\\s+slot|rank\\s+required|requires?\\s+(?:a\\s+)?(?:rank|skyblock\\s+level))");
    private static AbstractContainerScreen<?> cachedScreen;
    private static int cachedFingerprint;
    private static Resolution cachedResolution;

    private WardrobeSlotResolver() {}

    public static Resolution resolve(AbstractContainerScreen<?> screen){
        if(screen==null||!WardrobeMenuDetector.matches(screen))return empty(screen);
        int fingerprint=fingerprint(screen);
        if(screen==cachedScreen&&cachedResolution!=null&&fingerprint==cachedFingerprint)return cachedResolution;
        cachedScreen=screen;cachedFingerprint=fingerprint;cachedResolution=scan(screen);
        return cachedResolution;
    }

    public static void clearUnless(AbstractContainerScreen<?> screen){
        if(screen==cachedScreen)return;
        cachedScreen=null;cachedResolution=null;cachedFingerprint=0;
    }

    private static Resolution scan(AbstractContainerScreen<?> screen){
        Minecraft client=Minecraft.getInstance();
        WardrobeMenuDetector.Page page=WardrobeMenuDetector.page(screen);
        Map<Integer,ResolvedSlot> resolved=new LinkedHashMap<>();
        List<String> diagnostics=new ArrayList<>();
        for(int position=0;position<WardrobeMenuDetector.SELECTORS_PER_PAGE;position++){
            int menuSlot=WardrobeMenuDetector.FIRST_SELECTOR_SLOT+position;
            Slot slot=screen.getMenu().getSlot(menuSlot);
            if(client.player!=null&&slot.container==client.player.getInventory())continue;
            if(!slot.hasItem()){diagnostics.add("position "+(position+1)+" -> slot "+menuSlot+" ignored (missing selector)");continue;}
            ItemStack stack=slot.getItem();
            StringBuilder text=new StringBuilder(WardrobeMenuDetector.normalize(stack.getHoverName().getString()));
            for(Component line:Screen.getTooltipFromItem(client,stack))text.append('\n').append(WardrobeMenuDetector.normalize(line.getString()));
            Integer displayedNumber=parseNumber(text.toString());
            int actualNumber=displayedNumber!=null?displayedNumber:page==null?position+1:(page.current()-1)*WardrobeMenuDetector.SELECTORS_PER_PAGE+position+1;
            boolean locked=stack.is(Items.RED_DYE)||LOCKED.matcher(text).find();
            if(locked){diagnostics.add(actualNumber+" -> slot "+menuSlot+" ignored (locked/unavailable)");continue;}
            resolved.put(actualNumber,new ResolvedSlot(actualNumber,menuSlot,stack.getHoverName().getString(),displayedNumber!=null));
        }
        return new Resolution(true,screen.getTitle().getString(),screen.getMenu().slots.size(),page,Map.copyOf(resolved),List.copyOf(diagnostics));
    }

    private static Integer parseNumber(String text){
        Matcher forward=SLOT_NUMBER.matcher(text);
        if(forward.find())return parse(forward.group(1));
        Matcher reverse=REVERSED_SLOT_NUMBER.matcher(text);
        return reverse.find()?parse(reverse.group(1)):null;
    }

    private static Integer parse(String value){try{int parsed=Integer.parseInt(value);return parsed>=1&&parsed<=27?parsed:null;}catch(NumberFormatException ignored){return null;}}

    private static int fingerprint(AbstractContainerScreen<?> screen){
        int result=31+screen.getMenu().containerId;
        for(int index=0;index<WardrobeMenuDetector.FIRST_SELECTOR_SLOT+WardrobeMenuDetector.SELECTORS_PER_PAGE;index++){
            Slot slot=screen.getMenu().getSlot(index);result=31*result+ItemStack.hashItemAndComponents(slot.getItem());
        }
        return result;
    }

    private static Resolution empty(AbstractContainerScreen<?> screen){
        return new Resolution(false,screen==null?"none":screen.getTitle().getString(),screen==null?0:screen.getMenu().slots.size(),null,Map.of(),List.of());
    }

    public record ResolvedSlot(int wardrobeNumber,int menuSlot,String itemName,boolean numberFromText) {}
    public record Resolution(boolean detected,String title,int containerSlotCount,WardrobeMenuDetector.Page page,Map<Integer,ResolvedSlot> slots,List<String> diagnostics) {}
}
