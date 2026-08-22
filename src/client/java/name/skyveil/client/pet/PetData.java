package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.world.item.ItemStack;

/** Immutable snapshot of the best authoritative client-visible equipped-pet data. */
public record PetData(
    PetInstanceId instanceId,
    String internalId,
    String name,
    SkyblockRarity rarity,
    int level,
    boolean levelKnown,
    int maxLevel,
    double currentLevelXp,
    double xpForNextLevel,
    boolean xpKnown,
    boolean maxed,
    String petItemId,
    String petItemName,
    SkyblockRarity petItemRarity,
    int petItemRgb,
    ItemStack petIcon,
    ItemStack petItemIcon
) {
    public double xpRemaining(){return levelKnown&&xpKnown&&!maxed?Math.max(0,xpForNextLevel-currentLevelXp):0;}
    public double progress(){return levelKnown&&maxed?1:levelKnown&&xpKnown&&xpForNextLevel>0?Math.max(0,Math.min(1,currentLevelXp/xpForNextLevel)):0;}
    public boolean hasPetItem(){return petItemName!=null&&!petItemName.isBlank();}
}
