package name.skyveil.client.auction;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuctionPricesTest {
    @Test void finalTooltipRefreshReplacesCachedUnavailableAndDoesNotDuplicatePrices(){
        var lines=java.util.List.<net.minecraft.network.chat.Component>of(net.minecraft.network.chat.Component.literal("Blossom Necklace"),
            net.minecraft.network.chat.Component.literal("Full craft cost: Unavailable"));
        var value=net.minecraft.network.chat.Component.literal("Full craft cost: 20,000,000");
        var updated=AuctionTooltip.replacePriceLine(lines,"Full craft cost: ",value);
        assertEquals(2,updated.size());assertEquals(value,updated.get(1));
        updated=AuctionTooltip.replacePriceLine(updated,"Full craft cost: ",value);
        assertEquals(2,updated.size());
        var bin=net.minecraft.network.chat.Component.literal("Lowest BIN: 15,000,000");
        updated=AuctionTooltip.replacePriceLine(updated,"Lowest BIN: ",bin);
        updated=AuctionTooltip.replacePriceLine(updated,"Lowest BIN: ",bin);
        assertEquals(1,updated.stream().filter(line->line.getString().startsWith("Lowest BIN: ")).count());
    }
    @Test void innateKuudraAttributesAreNotPaidUpgrades(){
        var extra=new CompoundTag();extra.putString("id","AURORA_CHESTPLATE");
        var attributes=new CompoundTag();attributes.putInt("mana_pool",1);attributes.putInt("mana_regeneration",1);
        extra.put("attributes",attributes);
        assertTrue(AuctionPrices.isUnmodified(extra));
        attributes.putInt("mana_pool",2);assertFalse(AuctionPrices.isUnmodified(extra));
        attributes.putInt("mana_pool",1);extra.putInt("upgrade_level",1);
        assertFalse(AuctionPrices.isUnmodified(extra));
    }
    @Test void upgradedListingsAreExcludedFromTheUnmodifiedBasePrice(){
        var extra=new CompoundTag();extra.putString("id","SWORD");
        assertTrue(AuctionPrices.isUnmodified(extra));
        extra.putInt("hot_potato_count",0);assertTrue(AuctionPrices.isUnmodified(extra));
        extra.putInt("hot_potato_count",1);assertFalse(AuctionPrices.isUnmodified(extra));
        extra.remove("hot_potato_count");extra.putBoolean("artOfPeaceApplied",true);
        assertFalse(AuctionPrices.isUnmodified(extra));
    }
    @Test void onlyOpenBuyItNowListingsContribute(){
        var listing=JsonParser.parseString("""
            {"bin":true,"claimed":false,"end":2000}
            """).getAsJsonObject();
        assertTrue(AuctionPrices.eligible(listing,1000));
        assertFalse(AuctionPrices.eligible(listing,2000));
        listing.addProperty("bin",false);assertFalse(AuctionPrices.eligible(listing,1000));
        listing.addProperty("bin",true);listing.addProperty("claimed",true);
        assertFalse(AuctionPrices.eligible(listing,1000));
    }
    @Test void petsAreSeparatedBySpeciesAndRarity(){
        var extra=new CompoundTag();extra.putString("id","PET");
        extra.putString("petInfo","{\"type\":\"HEDGEHOG\",\"tier\":\"LEGENDARY\"}");
        assertEquals("PET:HEDGEHOG:LEGENDARY:LVL:1",AuctionPrices.identity(extra));
        extra.putString("petInfo","{\"type\":\"SKELETON\",\"tier\":\"EPIC\"}");
        assertEquals("PET:SKELETON:EPIC:LVL:1",AuctionPrices.identity(extra));
    }
    @Test void unknownVariantsAreNotPricedAsBaseItems(){
        var extra=new CompoundTag();extra.putString("id","ENCHANTED_BOOK");
        assertEquals("",AuctionPrices.identity(extra));
        extra.putString("id","POTION");assertEquals("",AuctionPrices.identity(extra));
        extra.putString("id","ASPECT_OF_THE_END");assertEquals("ASPECT_OF_THE_END",AuctionPrices.identity(extra));
    }
}