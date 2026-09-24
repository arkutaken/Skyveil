package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;
import name.skyveil.client.itemsearch.SkyBlockItemIconCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** Preserve the owned pet's skin; use the bundled species texture only when it is absent. */
final class PetHeadResolver {
    private PetHeadResolver(){}
    static ItemStack resolve(ItemStack owned,String type,String name,SkyblockRarity rarity){
        if(owned!=null&&!owned.isEmpty()){
            var profile=owned.get(DataComponents.PROFILE);
            if(profile!=null&&!profile.partialProfile().properties().get("textures").isEmpty())return owned.copy();
        }
        String species=(type==null||type.isBlank()?name:type).trim().toUpperCase(Locale.ROOT).replace(' ','_');
        int tier=rarity==null?4:Math.min(5,rarity.ordinal());
        ItemStack catalog=SkyBlockItemIconCatalog.resolve(species+";"+tier);
        if(catalog.isEmpty()&&tier!=4)catalog=SkyBlockItemIconCatalog.resolve(species+";4");
        return catalog;
    }
}