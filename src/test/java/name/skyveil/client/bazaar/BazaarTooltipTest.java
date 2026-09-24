package name.skyveil.client.bazaar;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BazaarTooltipTest {
    @Test void singleItemsDoNotRepeatTheirPrice(){
        assertEquals("Lowest BIN: 800,000",BazaarTooltip.priceLine("Lowest BIN",800000.0,1,0xFFAA00).getString());
    }
    @Test void matchesItemsInsideProtocolAndComponentWrappers(){
        var attributes=new CompoundTag();attributes.putString("id","ASPECT_OF_THE_END");
        var legacy=new CompoundTag();legacy.put("ExtraAttributes",attributes);
        var wrapped=new CompoundTag();wrapped.put("tag",legacy);
        assertEquals("ASPECT_OF_THE_END",BazaarTooltip.product(wrapped));
        var components=new CompoundTag();components.put("minecraft:custom_data",attributes);
        var modern=new CompoundTag();modern.put("components",components);
        assertEquals("ASPECT_OF_THE_END",BazaarTooltip.product(modern));
        assertEquals("ASPECT_OF_THE_END",name.skyveil.client.auction.AuctionPrices.identity(BazaarTooltip.attributes(modern)));
    }
    @Test void colorsOnlyLabelsAndDisplaysStackTotal(){
        var line=BazaarTooltip.priceLine("Insta Buy",6706.3,10,0xFFAA00);
        assertEquals("Insta Buy: 6,706.3 Total: 67,063",line.getString());
        var parts=line.getSiblings();
        assertEquals(0xFFAA00,parts.get(0).getStyle().getColor().getValue());
        assertEquals(0xFFFFFF,parts.get(1).getStyle().getColor().getValue());
        assertEquals(0xFFAA00,parts.get(2).getStyle().getColor().getValue());
        assertEquals(0xFFFFFF,parts.get(3).getStyle().getColor().getValue());
        assertEquals("Insta Sell: Unavailable Total: Unavailable",BazaarTooltip.priceLine("Insta Sell",null,10,0x55FF55).getString());
    }
    @Test void usesBestInstantPricesWithDecimalsAndHandlesEmptyMarkets(){
        var root=JsonParser.parseString("""
            {"products":{"WHEAT":{"buy_summary":[{"pricePerUnit":3.7,"amount":10},{"pricePerUnit":3.2,"amount":10}],
            "sell_summary":[{"pricePerUnit":2.1,"amount":10},{"pricePerUnit":2.5,"amount":10}]},
            "EMPTY":{"buy_summary":[],"sell_summary":[]}}}
            """).getAsJsonObject();
        var quotes=BazaarPrices.parse(root);
        assertEquals(3.2,quotes.get("WHEAT").buy());
        assertEquals(2.5,quotes.get("WHEAT").sell());
        assertNull(quotes.get("EMPTY").buy());
        assertNull(quotes.get("SWORD"));
    }
    @Test void distinguishesEnchantmentLevelsAndRejectsCombinedBooks(){
        var data=new CompoundTag();data.putString("id","ENCHANTED_BOOK");
        var enchants=new CompoundTag();enchants.putInt("sharpness",6);data.put("enchantments",enchants);
        assertEquals("ENCHANTMENT_SHARPNESS_6",BazaarTooltip.product(data));
        enchants.putInt("critical",6);assertEquals("",BazaarTooltip.product(data));
    }
    @Test void mapsNestedItemsAndLegacyDyes(){
        var root=new CompoundTag();var extra=new CompoundTag();extra.putString("id","COCOA_BEANS");
        root.put("ExtraAttributes",extra);
        assertEquals("INK_SACK:3",BazaarTooltip.product(root));
        assertEquals("",BazaarTooltip.product(new CompoundTag()));
    }
}