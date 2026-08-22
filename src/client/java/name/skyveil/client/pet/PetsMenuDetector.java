package name.skyveil.client.pet;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Locale;

/** Tolerates Hypixel formatting/page decorations while rejecting unrelated containers. */
public final class PetsMenuDetector {
    private PetsMenuDetector() {}

    public static boolean matches(AbstractContainerScreen<?> screen) {
        if(screen==null)return false;
        String title=screen.getTitle().getString().toLowerCase(Locale.ROOT)
            .replaceAll("(?:\\u00c2)?\\u00a7.","")
            .replaceAll("[^a-z0-9/() ]"," ").replaceAll("\\s+"," ").trim();
        return title.matches(".*\\bpets\\b.*")&&!title.contains("pet items");
    }
}
