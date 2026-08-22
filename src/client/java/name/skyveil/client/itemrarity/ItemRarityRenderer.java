package name.skyveil.client.itemrarity;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Draws only below item extraction; vanilla items and Skyveil lock icons render afterward. */
public final class ItemRarityRenderer {
    private ItemRarityRenderer() {}

    public static void drawBelowItem(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y) {
        var config=ConfigManager.get().itemRarity;
        if(!config.enabled||stack==null||stack.isEmpty()||SkyblockGuiItemDetector.isDecorative(stack))return;
        ItemRarityDetector.Highlight highlight=ItemRarityDetector.detectHighlight(stack);
        SkyblockRarity rarity=highlight==null?null:highlight.rarity();
        if(rarity==null)return;
        graphics.fill(x,y,x+16,y+16,(0x59<<24)|highlight.rgb());
    }
}
