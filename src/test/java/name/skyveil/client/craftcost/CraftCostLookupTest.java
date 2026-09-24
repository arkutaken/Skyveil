package name.skyveil.client.craftcost;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CraftCostLookupTest {
    @Test void normalizesRecipeProductsAndAuctionOnlyBooks(){
        assertEquals("INK_SACK:3",CraftCostTooltip.bazaarKey("COCOA_BEANS"));
        assertEquals("INK_SACK:4",CraftCostTooltip.bazaarKey("LAPIS_LAZULI"));
        assertEquals("BOOK:ULTIMATE_WISE=5",CraftCostTooltip.auctionKey("ENCHANTMENT_ULTIMATE_WISE_5"));
        assertEquals("BOOK:CRITICAL=7",CraftCostTooltip.auctionKey("ENCHANTMENT_CRITICAL_7"));
        assertEquals("SWORD",CraftCostTooltip.auctionKey("SWORD"));
    }
    @Test void incompleteRecipeCanUseUnmodifiedOutputMarketWithoutReturningPartialCost(){
        var catalog=JsonParser.parseString("{\"recipes\":{\"SWORD\":[{\"count\":1,\"ingredients\":{\"UNAVAILABLE\":2}}]}}").getAsJsonObject();
        var extra=new CompoundTag();extra.putString("id","SWORD");
        assertEquals(123.0,new CraftCostCalculator(catalog,id->id.equals("SWORD")?123.0:null).calculate(extra).coins());
        extra.putInt("hot_potato_count",1);
        var result=new CraftCostCalculator(catalog,id->id.equals("SWORD")?123.0:null).calculate(extra);
        assertNull(result.coins());assertTrue(result.missing().contains("HOT_POTATO_BOOK"));
    }
}
