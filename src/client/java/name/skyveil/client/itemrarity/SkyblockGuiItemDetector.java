package name.skyveil.client.itemrarity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Set;

/** Conservative filter for unmistakable Hypixel menu controls; real stored items remain eligible. */
public final class SkyblockGuiItemDetector {
    private static final Set<String> CONTROL_NAMES=Set.of(
        "close","go back","back","next page","previous page","search","sort","filter","empty slot"
    );

    private SkyblockGuiItemDetector() {}

    // Reject explicit control names/markers only. A real item can appear inside
    // a menu, so being a GUI stack is not by itself evidence of decoration.
    public static boolean isDecorative(ItemStack stack) {
        if(stack==null||stack.isEmpty())return true;
        String name=stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
        if(CONTROL_NAMES.contains(name))return true;
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        if(custom==null)return false;
        String data=custom.copyTag().toString().toUpperCase(Locale.ROOT);
        return data.contains("SKYBLOCK_MENU")||data.contains("MENU_GLASS")||data.contains("GUI_BUTTON")||data.contains("FILLER_ITEM");
    }
}
