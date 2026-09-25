package name.skyveil.client.wardrobe;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.regex.Pattern;

/** Identifies the armor Wardrobe using both its title and live numbered controls. */
public final class WardrobeMenuDetector {
    static final int FIRST_SELECTOR_SLOT=36;
    static final int SELECTORS_PER_PAGE=9;
    private static final Pattern ARMOR_SETS_TITLE=Pattern.compile("^\\(\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\)\\s*armor sets$",Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_TITLE=Pattern.compile("^wardrobe(?:\\s*\\(\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\))?$",Pattern.CASE_INSENSITIVE);
    private WardrobeMenuDetector() {}

    // Verify both title and selector-row evidence; an equipment menu or similarly
    // named container must not turn number keys into wardrobe actions.
    public static boolean matches(AbstractContainerScreen<?> screen){
        if(!titleMatches(screen)||screen.getMenu().slots.size()<FIRST_SELECTOR_SLOT+SELECTORS_PER_PAGE)return false;
        int layoutSlots=0,evidence=0;
        var player=MinecraftHolder.player();
        for(int index=FIRST_SELECTOR_SLOT;index<FIRST_SELECTOR_SLOT+SELECTORS_PER_PAGE;index++){
            var slot=screen.getMenu().getSlot(index);
            if(player!=null&&slot.container==player.getInventory())continue;
            layoutSlots++;
            if(!slot.hasItem())continue;
            String name=normalize(slot.getItem().getHoverName().getString());
            if(name.contains("slot")||slot.getItem().is(Items.GRAY_DYE)||slot.getItem().is(Items.RED_DYE)
                ||slot.getItem().is(Items.LIME_DYE)||slot.getItem().is(Items.GREEN_DYE))evidence++;
        }
        return layoutSlots==SELECTORS_PER_PAGE&&evidence>0;
    }

    public static boolean titleMatches(AbstractContainerScreen<?> screen){
        if(screen==null)return false;
        String title=normalize(screen.getTitle().getString());
        return !title.contains("equipment")&&(ARMOR_SETS_TITLE.matcher(title).matches()||LEGACY_TITLE.matcher(title).matches());
    }

    public static Page page(AbstractContainerScreen<?> screen){
        if(screen==null)return null;
        String title=normalize(screen.getTitle().getString());
        var current=ARMOR_SETS_TITLE.matcher(title);
        if(!current.matches())current=LEGACY_TITLE.matcher(title);
        if(!current.matches()||current.group(1)==null)return new Page(1,1);
        try {int page=Integer.parseInt(current.group(1)),pages=Integer.parseInt(current.group(2));return page>0&&pages>=page?new Page(page,pages):null;}
        catch(NumberFormatException ignored){return null;}
    }

    static String normalize(String value){
        return value==null?"":value.replaceAll("(?:\\u00c2)?\\u00a7.","")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/()# ]"," ").replaceAll("\\s+"," ").trim();
    }

    public record Page(int current,int total) {}

    /** Keeps Minecraft lookup isolated so menu detection remains easy to inspect and test. */
    private static final class MinecraftHolder {
        private static net.minecraft.client.player.LocalPlayer player(){return net.minecraft.client.Minecraft.getInstance().player;}
    }
}
