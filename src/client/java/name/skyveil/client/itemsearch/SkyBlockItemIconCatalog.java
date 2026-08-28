package name.skyveil.client.itemsearch;

import net.minecraft.world.item.ItemStack;

/** Public, exact-ID facade over the modeled stacks used by Item Search. */
public final class SkyBlockItemIconCatalog {
    private SkyBlockItemIconCatalog() {}

    public static ItemStack resolve(String internalId){return ItemSearchCatalog.stackByInternalName(internalId);}
}
