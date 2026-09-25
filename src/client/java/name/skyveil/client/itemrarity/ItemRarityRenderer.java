package name.skyveil.client.itemrarity;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Draws only below item extraction; vanilla items and Skyveil lock icons render afterward. */
public final class ItemRarityRenderer {
    private static final Map<ItemStack,CachedBackground> BACKGROUNDS=Collections.synchronizedMap(new WeakHashMap<>());

    private ItemRarityRenderer() {}

    public static void drawBelowItem(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y) {
        drawBelowItem(graphics,stack,x,y,null);
    }

    public static void drawBelowItem(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y,SkyblockRarity fallbackRarity) {
        var config=ConfigManager.get().itemRarity;
        if(!config.enabled||stack==null||stack.isEmpty())return;
        // The fallback rarity is part of the cache key: the same stack can gain
        // more authoritative pet-menu evidence without its components changing.
        int fingerprint=ItemStack.hashItemAndComponents(stack),fallbackRgb=fallbackRarity==null?-1:fallbackRarity.rgb();
        CachedBackground cached=BACKGROUNDS.get(stack);
        if(cached==null||cached.fingerprint()!=fingerprint||cached.fallbackRgb()!=fallbackRgb){
            int color=-1;ItemRarityDetector.Highlight highlight=ItemRarityDetector.detectHighlight(stack);
            // The visible, correctly colored tooltip rarity footer is authoritative. Real Pets
            // menu entries also carry GUI metadata, so rejecting them before reading that footer
            // incorrectly removes their rarity background.
            if(highlight!=null&&highlight.rarity()!=null)color=(0x59<<24)|highlight.rgb();
            else if(fallbackRarity!=null)color=(0x59<<24)|fallbackRarity.rgb();
            cached=new CachedBackground(fingerprint,fallbackRgb,color);BACKGROUNDS.put(stack,cached);
        }
        if(cached.color()!=-1)graphics.fill(x,y,x+16,y+16,cached.color());
    }

    private record CachedBackground(int fingerprint,int fallbackRgb,int color){}
}
