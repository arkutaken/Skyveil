package name.skyveil.client.inventorybuttons;

import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

/** Recognizes the container-backed terminals used in Floor 7 and Master Mode 7. */
public final class DungeonTerminalDetector {
    private DungeonTerminalDetector() {}

    public static boolean matches(Screen screen) {
        return screen!=null&&matchesTitle(screen.getTitle().getString());
    }

    // Strip server formatting before matching known puzzle titles. Keep this list
    // specific so ordinary inventories do not lose their command buttons.
    static boolean matchesTitle(String title) {
        String value=(title==null?"":title).replaceAll("(?:\\u00c2)?\\u00a7.","")
            .toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();
        return value.equals("correct all the panes!")
            ||value.equals("navigate the maze!")
            ||value.equals("click in order!")
            ||value.equals("change all to same color!")
            ||value.equals("click the button on time!")
            ||value.equals("align all the arrows!")
            ||value.startsWith("what starts with:")
            ||value.startsWith("starts with:")
            ||value.startsWith("select all the ")&&value.endsWith(" items!");
    }
}
