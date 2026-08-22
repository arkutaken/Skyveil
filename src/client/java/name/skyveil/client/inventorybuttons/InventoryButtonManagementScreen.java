package name.skyveil.client.inventorybuttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Central list for adding, editing, and deleting inventory buttons. */
public final class InventoryButtonManagementScreen {
    private InventoryButtonManagementScreen() {}
    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if(client.player==null)return;
        InventoryScreen screen=new InventoryScreen(client.player);
        InventoryButtonManager.beginManagement(screen);
        client.setScreen(screen);
    }
}
