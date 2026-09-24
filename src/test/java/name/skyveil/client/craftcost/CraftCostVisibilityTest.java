package name.skyveil.client.craftcost;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CraftCostVisibilityTest {
    private final CraftCostVisibility visibility=new CraftCostVisibility(CraftCostTooltip.catalog());
    private static CompoundTag item(String id){
        var extra=new CompoundTag();extra.putString("id",id);return extra;
    }
    @Test void noncraftableDropMaterialsDoNotShowCraftCost(){
        assertFalse(visibility.shouldShow(item("NECRON_HANDLE")));
        assertFalse(visibility.shouldShow(item("JUDGEMENT_CORE")));
        assertFalse(visibility.shouldShow(item("WITHER_CHESTPLATE")));
    }
    @Test void craftedAndForgedItemsKeepCraftCostEvenWhenAlsoIngredients(){
        assertTrue(visibility.shouldShow(item("HYPERION")));
        assertTrue(visibility.shouldShow(item("TITANIUM_DRILL_4")));
        assertTrue(visibility.shouldShow(item("HOT_AURORA_CHESTPLATE")));
    }
    @Test void appliedUpgradesPreserveCostOnObtainedItems(){
        var extra=item("WITHER_CHESTPLATE");
        extra.putInt("rarity_upgrades",1);assertTrue(visibility.shouldShow(extra));
        extra=item("WITHER_CHESTPLATE");
        extra.putString("modifier","ancient");assertTrue(visibility.shouldShow(extra));
        extra=item("WITHER_CHESTPLATE");
        var enchants=new CompoundTag();enchants.putInt("growth",6);extra.put("enchantments",enchants);
        assertTrue(visibility.shouldShow(extra));
        extra=item("WITHER_CHESTPLATE");
        extra.putInt("upgrade_level",5);assertTrue(visibility.shouldShow(extra));
    }
    @Test void metadataAndZeroUpgradeCountersDoNotMakeAMaterialUpgraded(){
        var extra=item("NECRON_HANDLE");
        extra.putString("uuid","instance");extra.putBoolean("donated_museum",true);
        extra.putInt("rarity_upgrades",0);extra.put("enchantments",new CompoundTag());
        assertFalse(visibility.shouldShow(extra));
    }
    @Test void unrelatedNonmaterialDropsAreOutsideThisExclusion(){
        assertTrue(visibility.shouldShow(item("GIANTS_SWORD")));
        assertTrue(visibility.shouldShow(item("UNKNOWN_ITEM")));
    }
}
