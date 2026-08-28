package name.skyveil.client.pet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Locale;
import java.util.WeakHashMap;

/** Draws a pet's parsed level in the bottom-right corner of supported menu icons. */
public final class PetMenuLevelOverlayRenderer {
    private static final Map<ItemStack,Integer> LEVEL_CACHE=new WeakHashMap<>();
    private static AbstractContainerScreen<?> cachedScreen;
    private static String cachedTitle="";
    private static boolean cachedSupported;

    private PetMenuLevelOverlayRenderer() {}

    public static void draw(AbstractContainerScreen<?> screen,GuiGraphicsExtractor graphics,Slot slot) {
        if(screen==null||slot==null||!slot.hasItem()||!supports(screen))return;
        Minecraft client=Minecraft.getInstance();
        if(client.player!=null&&slot.container==client.player.getInventory())return;
        int level=LEVEL_CACHE.computeIfAbsent(slot.getItem(),PetTracker::displayedMenuLevel);
        if(level<=0)return;
        String text=Integer.toString(level);
        int x=slot.x+17-client.font.width(text);
        int y=slot.y+9;
        graphics.text(client.font,text,x,y,0xFFFFFFFF,true);
    }

    private static boolean supports(AbstractContainerScreen<?> screen) {
        String title=screen.getTitle().getString();
        if(screen==cachedScreen&&title.equals(cachedTitle))return cachedSupported;
        if(screen!=cachedScreen)LEVEL_CACHE.clear();
        cachedScreen=screen;cachedTitle=title;
        cachedSupported=PetsMenuDetector.matches(screen)||isAuctionTitle(title);
        return cachedSupported;
    }

    static boolean isAuctionTitle(String title) {
        if(title==null)return false;
        String normalized=title.toLowerCase(Locale.ROOT)
            .replaceAll("(?:\\u00c2)?\\u00a7.","")
            .replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        return normalized.contains("auction");
    }
}
