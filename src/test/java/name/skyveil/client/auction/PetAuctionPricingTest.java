package name.skyveil.client.auction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PetAuctionPricingTest {
    private static CompoundTag pet(double xp,String held){
        var extra=new CompoundTag();extra.putString("id","PET");
        extra.putString("petInfo","{\"type\":\"SHEEP\",\"tier\":\"LEGENDARY\",\"exp\":"+xp+",\"heldItem\":\""+held+"\"}");
        return extra;
    }
    @Test void groupsSameLevelDespiteDifferentXpAndHeldItems(){
        var first=pet(0,"PET_ITEM_TIER_BOOST");var second=pet(50,"PET_ITEM_TEXTBOOK");
        assertEquals("PET:SHEEP:LEGENDARY:LVL:1",AuctionPrices.identity(first));
        assertEquals(AuctionPrices.identity(first),AuctionPrices.identity(second));
        assertEquals(AuctionSimilarity.key(first),AuctionSimilarity.key(second));
    }
    @Test void levelsNeverShareLowestBinOrHistory(){
        var low=pet(0,"");var high=pet(100000000,"");
        assertEquals("PET:SHEEP:LEGENDARY:LVL:100",AuctionPrices.identity(high));
        assertNotEquals(AuctionPrices.identity(low),AuctionPrices.identity(high));
        assertNotEquals(AuctionSimilarity.key(low),AuctionSimilarity.key(high));
        assertEquals("",AuctionPrices.identity(pet(-1,"")));
    }
}
