package name.skyveil.client.itemprotection;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ItemProtectionManager {
    private static long lastBlockedFeedback;
    private static int pendingLinkSlot=-1,lockPressSlot=-1;
    private static boolean lockKeyDown,linkUsedDuringPress;
    private ItemProtectionManager(){}
    public static boolean isLocked(int inventorySlot){return ConfigManager.get().lockedInventorySlots.contains(inventorySlot);}
    public static boolean isProtected(int inventorySlot){return ConfigManager.get().itemProtection.enabled&&isLocked(inventorySlot);}
    // Menu slot numbers vary by container. Persist only indices from the player's
    // own inventory; -1 means this slot is outside that supported namespace.
    public static int playerInventoryIndex(Slot slot){
        Minecraft client=Minecraft.getInstance();
        return client.player!=null&&slot!=null&&slot.container==client.player.getInventory()?slot.getContainerSlot():-1;
    }
    public static boolean isLocked(Slot slot){int index=playerInventoryIndex(slot);return index>=0&&isLocked(index);}
    public static boolean lockKeyHeld(){
        Minecraft client=Minecraft.getInstance();int key=ConfigManager.get().itemProtection.lockKey;
        return key>=0&&GLFW.glfwGetKey(client.getWindow().handle(),key)==GLFW.GLFW_PRESS;
    }
    public static boolean beginLockKeyPress(Slot hovered){
        if(lockKeyDown)return true;
        int index=playerInventoryIndex(hovered);
        lockKeyDown=true;linkUsedDuringPress=false;lockPressSlot=index>=0&&((hovered!=null&&hovered.hasItem())||isLocked(index))?index:-1;return true;
    }
    // Delay the toggle until release so using the same press for linking does not
    // also change the slot's lock state.
    public static boolean endLockKeyPress(){
        if(!lockKeyDown)return false;
        lockKeyDown=false;int toggleSlot=lockPressSlot;lockPressSlot=-1;pendingLinkSlot=-1;
        if(!linkUsedDuringPress&&toggleSlot>=0)toggle(toggleSlot);
        linkUsedDuringPress=false;return true;
    }
    public static void toggle(int inventorySlot){
        if(inventorySlot<0)return;
        if(!ConfigManager.get().lockedInventorySlots.remove(inventorySlot))ConfigManager.get().lockedInventorySlots.add(inventorySlot);
        ConfigManager.save();
    }
    public static void toggle(Slot slot){toggle(playerInventoryIndex(slot));}
    public static boolean handleLinkClick(Slot slot){
        int index=playerInventoryIndex(slot);if(!linkable(index))return false;
        linkUsedDuringPress=true;
        if(pendingLinkSlot<0){pendingLinkSlot=index;chat("Link selected. Click a "+(isHotbar(index)?"main inventory":"hotbar")+" slot while holding the lock key.");return true;}
        if(isHotbar(pendingLinkSlot)==isHotbar(index)){pendingLinkSlot=index;chat("Select a slot from the other inventory section.");return true;}
        int hotbar=isHotbar(pendingLinkSlot)?pendingLinkSlot:index;
        int inventory=isHotbar(pendingLinkSlot)?index:pendingLinkSlot;
        var links=ConfigManager.get().itemProtection.slotLinks;
        List<Integer> destinations=links.computeIfAbsent(hotbar,ignored->new ArrayList<>());
        if(destinations.remove(Integer.valueOf(inventory))){if(destinations.isEmpty())links.remove(hotbar);chat("Slot link removed.");}
        else{
            for(var entry:new ArrayList<>(links.entrySet())){
                entry.getValue().remove(Integer.valueOf(inventory));
                if(entry.getValue().isEmpty())links.remove(entry.getKey());
            }
            links.computeIfAbsent(hotbar,ignored->new ArrayList<>()).add(inventory);chat("Hotbar and inventory slots linked.");
        }
        pendingLinkSlot=hotbar;ConfigManager.save();return true;
    }
    public static int linkedHotbar(int inventorySlot){
        if(isHotbar(inventorySlot)&&ConfigManager.get().itemProtection.slotLinks.containsKey(inventorySlot))return inventorySlot;
        for(var entry:ConfigManager.get().itemProtection.slotLinks.entrySet())if(entry.getValue().contains(inventorySlot))return entry.getKey();
        return -1;
    }
    public static List<Integer> linkedInventorySlots(int hotbar){
        List<Integer> destinations=ConfigManager.get().itemProtection.slotLinks.get(hotbar);
        return destinations==null?List.of():destinations;
    }
    public static boolean isLinked(int inventorySlot){return linkedHotbar(inventorySlot)>=0;}
    public static boolean isPending(int inventorySlot){return inventorySlot==pendingLinkSlot;}
    public static void clearPendingLink(){pendingLinkSlot=lockPressSlot=-1;lockKeyDown=linkUsedDuringPress=false;}

    /** Redirects an explicit Shift-click through vanilla's atomic hotbar SWAP action. */
    public static boolean redirectLinkedQuickMove(AbstractContainerMenu menu,Slot clicked){
        int sourceIndex=playerInventoryIndex(clicked),hotbar=linkedHotbar(sourceIndex);
        if(hotbar<0)return false;
        Minecraft client=Minecraft.getInstance();if(client.player==null||client.gameMode==null)return true;
        if(clicked==null||!clicked.hasItem()||!menu.getCarried().isEmpty())return true;
        Slot inventorySlot;
        if(isHotbar(sourceIndex))inventorySlot=preferredDestination(menu,hotbar,clicked.getItem());
        else inventorySlot=clicked;
        Slot hotbarSlot=findPlayerSlot(menu,hotbar);
        if(inventorySlot==null||hotbarSlot==null)return true;
        if(isLocked(hotbarSlot)||isLocked(inventorySlot)){blocked();return true;}
        if(!hotbarSlot.mayPickup(client.player)||!inventorySlot.mayPickup(client.player)
            ||!inventorySlot.mayPlace(hotbarSlot.getItem())||!hotbarSlot.mayPlace(inventorySlot.getItem()))return true;
        // SWAP expects the GUI slot id of the main-inventory side and the player hotbar index.
        client.gameMode.handleContainerInput(menu.containerId,inventorySlot.index,hotbar,ContainerInput.SWAP,client.player);
        return true;
    }

    private static Slot preferredDestination(AbstractContainerMenu menu,int hotbar,net.minecraft.world.item.ItemStack source){
        Slot firstValid=null;Minecraft client=Minecraft.getInstance();
        for(Integer index:linkedInventorySlots(hotbar)){
            Slot candidate=findPlayerSlot(menu,index);if(candidate==null||client.player==null||!candidate.mayPickup(client.player)||!candidate.mayPlace(source))continue;
            if(firstValid==null)firstValid=candidate;if(!candidate.hasItem())return candidate;
        }
        return firstValid;
    }
    private static Slot findPlayerSlot(AbstractContainerMenu menu,int inventoryIndex){
        for(Slot candidate:menu.slots)if(playerInventoryIndex(candidate)==inventoryIndex)return candidate;return null;
    }
    private static boolean linkable(int index){return isHotbar(index)||index>=9&&index<=35;}
    private static boolean isHotbar(int index){return index>=0&&index<=8;}
    public static void blocked(){
        if(!ConfigManager.get().itemProtection.feedback)return;
        long now=System.currentTimeMillis();if(now-lastBlockedFeedback<350)return;lastBlockedFeedback=now;
        show("Skyveil: This item is locked.");
    }
    private static void show(String text){Minecraft client=Minecraft.getInstance();if(client.gui!=null)client.gui.setOverlayMessage(Component.literal(text),false);}
    static void chat(String text){Minecraft client=Minecraft.getInstance();if(client.player!=null)client.player.sendSystemMessage(gradientPrefix().append(Component.literal(" "+text).withStyle(ChatFormatting.WHITE)));}
    private static MutableComponent gradientPrefix(){
        String prefix="[Skyveil]";int dark=0x673AB7,bright=0xC995FF;MutableComponent result=Component.empty();
        for(int i=0;i<prefix.length();i++){
            double t=i/(double)(prefix.length()-1);int r=(int)Math.round(((dark>>16)&255)*(1-t)+((bright>>16)&255)*t),g=(int)Math.round(((dark>>8)&255)*(1-t)+((bright>>8)&255)*t),b=(int)Math.round((dark&255)*(1-t)+(bright&255)*t);
            result.append(Component.literal(String.valueOf(prefix.charAt(i))).withStyle(style->style.withColor(TextColor.fromRgb((r<<16)|(g<<8)|b))));
        }
        return result;
    }
}
