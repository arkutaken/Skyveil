package name.skyveil.client.craftcost;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TableEnchantmentPricingTest {
    @Test void screenshotTableEnchantmentsNeverRequestMarketPrices(){
        var extra=new CompoundTag();extra.putString("id","TEST_SWORD");
        var enchants=new CompoundTag();
        enchants.putInt("fire_aspect",2);enchants.putInt("dragon_hunter",5);
        enchants.putInt("magmarizer",5);enchants.putInt("impaling",5);
        extra.put("enchantments",enchants);
        var requested=new ArrayList<String>();
        var result=new CraftCostCalculator(CraftCostTooltip.catalog(),id->{requested.add(id);return id.equals("TEST_SWORD")?100.0:null;}).calculate(extra);
        assertEquals(100.0,result.coins());assertEquals(List.of("TEST_SWORD"),requested);
        assertTrue(CraftCostTooltip.paidEnchantments(extra).isEmpty());
        assertTrue(name.skyveil.client.auction.AuctionPrices.isUnmodified(extra));
        enchants.putInt("ultimate_wise",5);enchants.putInt("fire_aspect",3);
        assertEquals(Set.of("ultimate_wise","fire_aspect"),CraftCostTooltip.paidEnchantments(extra).keySet());
        assertFalse(name.skyveil.client.auction.AuctionPrices.isUnmodified(extra));
    }
    @Test void enchantedBooksKeepTheirEnchantmentIdentity(){
        var extra=new CompoundTag();extra.putString("id","ENCHANTED_BOOK");
        var enchants=new CompoundTag();enchants.putInt("fire_aspect",2);extra.put("enchantments",enchants);
        assertEquals(enchants,CraftCostTooltip.paidEnchantments(extra));
    }
    @Test void warpedStoneModifierUsesWarpedCostAndApplicationFee(){
        var extra=new CompoundTag();extra.putString("id","ASPECT_OF_THE_VOID");extra.putString("modifier","warped");
        var calculator=new CraftCostCalculator(CraftCostTooltip.catalog(),id->100.0);
        var expected=calculator.calculate(extra);assertNotNull(expected.coins());
        extra.putString("modifier","aote_stone");
        assertEquals(expected,calculator.calculate(extra));
    }
    @Test void unavailableSummaryStaysShortRegardlessOfInputNames(){
        assertEquals(" (5 unpriced inputs)",CraftCostTooltip.missingSummary(5));
        assertTrue(CraftCostTooltip.missingSummary(1000).length()<30);
    }
}
