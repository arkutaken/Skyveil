package name.skyveil.client.itemprotection;

import name.skyveil.client.config.ConfigManager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import java.util.Locale;

/** Drop-only protection travels with the item's server UUID, never its inventory position. */
public final class ProtectedItemManager {
    private static boolean keyDown;
    public static void releaseKey(){keyDown=false;}
    public static void press(ItemStack stack){if(!keyDown){keyDown=true;toggle(stack);}}
    private ProtectedItemManager(){}
    static String key(CompoundTag data,String base){
        var extra=data.getCompoundOrEmpty("ExtraAttributes");
        String uuid=data.getStringOr("uuid",extra.getStringOr("uuid",""));
        if(!uuid.isBlank())return "uuid:"+uuid.toLowerCase(Locale.ROOT);
        String id=data.getStringOr("id",extra.getStringOr("id",""));
        // Stackable items have no individual identity; protect every copy of that item type.
        return "type:"+(id.isBlank()?base:id);
    }
    public static String key(ItemStack stack){
        if(stack==null||stack.isEmpty())return "";
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return key(data==null?new CompoundTag():data.copyTag(),BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
    public static boolean protectedItem(ItemStack stack){
        return ConfigManager.get().itemProtection.protectItems&&ConfigManager.get().itemProtection.protectedItems.contains(key(stack));
    }
    public static void toggle(ItemStack stack){
        String key=key(stack);if(key.isBlank())return;
        var config=ConfigManager.get().itemProtection;
        boolean added=!config.protectedItems.remove(key);
        if(added)config.protectedItems.add(key);
        ConfigManager.save();
        ItemProtectionManager.chat(added?"Item protected!":"Item not protected!");
    }
    public static boolean drops(ContainerInput input,int slot){
        return input==ContainerInput.THROW||input==ContainerInput.PICKUP&&slot==-999;
    }
    private static long lastFeedback;
    public static void blocked(){
        long now=System.currentTimeMillis();
        if(now-lastFeedback<750)return;
        lastFeedback=now;
        ItemProtectionManager.chat("This item is protected from dropping.");
    }
}