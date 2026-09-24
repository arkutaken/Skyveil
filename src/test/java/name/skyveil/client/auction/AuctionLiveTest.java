package name.skyveil.client.auction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
/** Opt-in public API integration test; ordinary release builds remain fully offline. */
@EnabledIfEnvironmentVariable(named="SKYVEIL_LIVE_AUCTIONS",matches="1")
class AuctionLiveTest {
    @Test void publicSnapshotContainsMultipleItemFamilies() throws Exception {
        AuctionPrices.refresh();
        var prices=AuctionPrices.current().prices();
        System.out.println("LIVE AUCTION KEYS: "+prices.size()+" unmodified="+AuctionPrices.current().unmodified().size());
        assertFalse(AuctionPrices.current().similar().isEmpty());
        System.out.println("LIVE UPGRADE CONFIGURATIONS: "+AuctionPrices.current().similar().size());
        var history=new AuctionHistory();
        history.update(AuctionPrices.current().generation(),AuctionPrices.current().similar(),System.currentTimeMillis());
        assertTrue(history.saveForCache().sizeInBytes()<8*1024*1024);
        assertTrue(prices.size()>100,"Expected broad coverage, found "+prices.size());
        for(String key:java.util.List.of("ASPECT_OF_THE_END","HYPERION","WITHER_BOOTS","BLOSSOM_NECKLACE")){
            assertTrue(prices.containsKey(key),"Missing common auction item "+key);
            System.out.println(key+": "+prices.get(key));
        }
        var response=java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
            java.net.URI.create("https://api.hypixel.net/v2/skyblock/bazaar")).timeout(java.time.Duration.ofSeconds(20)).build(),java.net.http.HttpResponse.BodyHandlers.ofString());
        var market=name.skyveil.client.bazaar.BazaarPrices.parse(com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject());
        try(var stream=getClass().getResourceAsStream("/assets/skyveil/data/craft_cost_catalog.json")){
            var catalog=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
            var extra=new net.minecraft.nbt.CompoundTag();extra.putString("id","BLOSSOM_NECKLACE");extra.putString("modifier","thorny");
            var enchants=new net.minecraft.nbt.CompoundTag();enchants.putInt("green_thumb",3);extra.put("enchantments",enchants);
            var result=new name.skyveil.client.craftcost.CraftCostCalculator(catalog,id->{
                var quote=market.get(id);return quote==null?AuctionPrices.current().unmodified().get(id):quote.buy();
            }).calculate(extra);
            System.out.println("BLOSSOM CRAFT: "+result+" base="+AuctionPrices.current().unmodified().get("BLOSSOM_NECKLACE"));
            assertNotNull(result.coins(),result.toString());
            extra=new net.minecraft.nbt.CompoundTag();extra.putString("id","TERMINATOR");extra.putString("modifier","precise");
            extra.putInt("rarity_upgrades",1);extra.putInt("upgrade_level",5);
            enchants=new net.minecraft.nbt.CompoundTag();
            for(var entry:java.util.Map.ofEntries(java.util.Map.entry("ultimate_reiterate",5),java.util.Map.entry("toxophilite",10),
                java.util.Map.entry("chance",4),java.util.Map.entry("cubism",5),java.util.Map.entry("aiming",5),java.util.Map.entry("flame",1),
                java.util.Map.entry("impaling",1),java.util.Map.entry("infinite_quiver",10),java.util.Map.entry("overload",5),
                java.util.Map.entry("piercing",1),java.util.Map.entry("power",6),java.util.Map.entry("snipe",3)).entrySet())enchants.putInt(entry.getKey(),entry.getValue());
            extra.put("enchantments",enchants);
            result=new name.skyveil.client.craftcost.CraftCostCalculator(catalog,id->{
                var quote=market.get(id);return quote==null?AuctionPrices.current().unmodified().get(id):quote.buy();
            }).calculate(extra);
            System.out.println("TERMINATOR CRAFT: "+result);
            assertNotNull(result.coins(),result.toString());
            extra=new net.minecraft.nbt.CompoundTag();extra.putString("id","FIERY_AURORA_CHESTPLATE");
            extra.putString("modifier","loving");extra.putInt("rarity_upgrades",1);
            enchants=new net.minecraft.nbt.CompoundTag();
            for(String enchant:java.util.List.of("ultimate_wisdom","growth","protection","rejuvenate","vampirism"))enchants.putInt(enchant,5);
            extra.put("enchantments",enchants);
            var gems=new net.minecraft.nbt.CompoundTag();
            for(int slot=0;slot<2;slot++){gems.putString("COMBAT_"+slot,"PERFECT");gems.putString("COMBAT_"+slot+"_gem","SAPPHIRE");}
            extra.put("gems",gems);
            result=new name.skyveil.client.craftcost.CraftCostCalculator(catalog,id->{
                var quote=market.get(id);return quote==null?AuctionPrices.current().unmodified().get(id):quote.buy();
            }).calculate(extra);
            System.out.println("FIERY AURORA CRAFT: "+result);
            assertNotNull(result.coins(),result.toString());
            assertTrue(result.materials().get("KUUDRA_TEETH")>0);
        }
        assertTrue(prices.keySet().stream().anyMatch(key->key.startsWith("PET:")));
        assertTrue(prices.keySet().stream().anyMatch(key->key.startsWith("RUNE:")));
    }
}