package name.skyveil.client.itemsearch;

import net.minecraft.world.item.ItemStack;

/** Public, exact-ID facade over the modeled stacks used by Item Search. */
public final class SkyBlockItemIconCatalog {
    private SkyBlockItemIconCatalog() {}

    // Exact internal IDs avoid fuzzy name collisions; the catalog returns a copy
    // so callers can change display components without changing its cached template.
    public static ItemStack resolve(String internalId){return ItemSearchCatalog.stackByInternalName(internalId);}
}
