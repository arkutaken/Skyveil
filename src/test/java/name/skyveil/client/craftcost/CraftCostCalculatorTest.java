package name.skyveil.client.craftcost;
import com.google.gson.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CraftCostCalculatorTest {
    private static JsonObject catalog(){return JsonParser.parseString("""
        {"recipes":{"SWORD":[{"count":1,"ingredients":{"INGREDIENT":2}}]},
         "items":{"SWORD":{"tier":"RARE","gemstone_slots":[{"slot_type":"COMBAT","costs":[{"type":"COINS","coins":7}]}],
             "upgrade_costs":[[{"type":"ESSENCE","essence_type":"WITHER","amount":3}]]}},
         "reforges":{"ancient":{"type":"blacksmith/reforge_stone","item":"STONE","costs":{"RARE":5,"EPIC":6}}},
         "tableEnchants":{"SHARPNESS":5}}
        """).getAsJsonObject();}
    private static CompoundTag item(){var extra=new CompoundTag();extra.putString("id","SWORD");return extra;}
    @Test void sumsRecipeAndAllAppliedUpgradeCategoriesExactly(){
        var extra=item();extra.putInt("hot_potato_count",12);extra.putInt("rarity_upgrades",1);
        extra.putString("modifier","ancient");extra.putInt("upgrade_level",1);extra.putInt("art_of_war_count",1);
        var enchants=new CompoundTag();enchants.putInt("sharpness",5);enchants.putInt("ultimate_wise",2);extra.put("enchantments",enchants);
        var gems=new CompoundTag();gems.putString("COMBAT_0","PERFECT");gems.putString("COMBAT_0_gem","SAPPHIRE");
        var unlocked=new ListTag();unlocked.add(StringTag.valueOf("COMBAT_0"));gems.put("unlocked_slots",unlocked);extra.put("gems",gems);
        Map<String,Double> prices=Map.ofEntries(Map.entry("INGREDIENT",10.0),Map.entry("HOT_POTATO_BOOK",2.0),
            Map.entry("FUMING_POTATO_BOOK",3.0),Map.entry("RECOMBOBULATOR_3000",4.0),Map.entry("STONE",8.0),
            Map.entry("ESSENCE_WITHER",2.0),Map.entry("PERFECT_SAPPHIRE_GEM",9.0),
            Map.entry("ENCHANTMENT_ULTIMATE_WISE_1",5.0),Map.entry("THE_ART_OF_WAR",11.0));
        var result=new CraftCostCalculator(catalog(),prices::get).calculate(extra);
        assertEquals(107.0,result.coins()); // 20+20+6+4+8+6+6+9+7+10+11
        assertTrue(result.missing().isEmpty());
    }
    @Test void missingPriceNeverBecomesAPartialFullCost(){
        var extra=item();extra.putInt("hot_potato_count",1);
        var result=new CraftCostCalculator(catalog(),id->id.equals("INGREDIENT")?10.0:null).calculate(extra);
        assertNull(result.coins());assertTrue(result.missing().contains("HOT_POTATO_BOOK"));
    }
    @Test void zeroAndTenPotatoesDoNotChargeFumingBooks(){
        var extra=item();extra.putInt("hot_potato_count",10);
        var result=new CraftCostCalculator(catalog(),id->id.equals("FUMING_POTATO_BOOK")?null:1.0).calculate(extra);
        assertEquals(12.0,result.coins());
    }
    @Test void outputQuantityAndRecipeCyclesAreHandled(){
        var data=JsonParser.parseString("""
            {"recipes":{"SWORD":[{"count":4,"ingredients":{"ORE":8}}]},"items":{}}
            """).getAsJsonObject();
        assertEquals(10.0,new CraftCostCalculator(data,id->id.equals("ORE")?5.0:null).calculate(item()).coins());
        data=JsonParser.parseString("""
            {"recipes":{"SWORD":[{"count":1,"ingredients":{"SWORD":1}}]},"items":{}}
            """).getAsJsonObject();
        assertNull(new CraftCostCalculator(data,id->null).calculate(item()).coins());
    }
    @Test void leveledEnchantmentsChargeOneBookAndTableEnchantmentsNoCoins(){
        var extra=item();var enchants=new CompoundTag();enchants.putInt("champion",10);enchants.putInt("sharpness",5);
        extra.put("enchantments",enchants);
        assertEquals(12.0,new CraftCostCalculator(catalog(),id->id.equals("INGREDIENT")?1.0:id.equals("ENCHANTMENT_CHAMPION_1")?10.0:null).calculate(extra).coins());
    }
    @Test void compoundGemQualityIsSupported(){
        var extra=item();var gems=new CompoundTag();var gem=new CompoundTag();gem.putString("quality","PERFECT");
        gems.put("COMBAT_0",gem);gems.putString("COMBAT_0_gem","SAPPHIRE");extra.put("gems",gems);
        assertEquals(10.0,new CraftCostCalculator(catalog(),id->1.0).calculate(extra).coins());
    }
    @Test void actualAppliedFlagsAndPowerScrollIdsAreNotDroppedOrDuplicated(){
        var extra=item();extra.putBoolean("artOfPeaceApplied",true);extra.putBoolean("stats_book",true);
        extra.putString("power_ability_scroll","SAPPHIRE_POWER_SCROLL");
        Map<String,Double> prices=Map.of("INGREDIENT",1.0,"THE_ART_OF_PEACE",10.0,"BOOK_OF_STATS",20.0,"SAPPHIRE_POWER_SCROLL",30.0);
        assertEquals(62.0,new CraftCostCalculator(catalog(),prices::get).calculate(extra).coins());
    }
    @Test void silexCountsDoNotExceedAppliedEfficiencyUpgrades(){
        var extra=item();var enchantments=new CompoundTag();enchantments.putInt("efficiency",10);extra.put("enchantments",enchantments);
        assertEquals(52.0,new CraftCostCalculator(catalog(),id->id.equals("INGREDIENT")?1.0:id.equals("SILEX")?10.0:null).calculate(extra).coins());
    }
    @Test void bloodShotUsesShriveledCorneaAndItsApplicationFee(){
        var data=CraftCostTooltip.catalog();
        var extra=new CompoundTag();extra.putString("id","MYTHOS_BELT");
        var requested=new HashSet<String>();
        var calculator=new CraftCostCalculator(data,id->{requested.add(id);return 100.0;});
        Double base=calculator.calculate(extra).coins();
        assertNotNull(base);
        extra.putString("modifier","blood_shot");
        assertEquals(base+100+1_000_000,calculator.calculate(extra).coins());
        assertTrue(requested.contains("SHRIVELED_CORNEA"));
        assertFalse(requested.contains("BLOOD_SHOT"));
        extra.putString("modifier","Bloodshot");
        assertEquals(base+100+1_000_000,calculator.calculate(extra).coins());
    }
    @Test void basicBowTableEnchantmentsDoNotRequireBazaarOrders(){
        var data=CraftCostTooltip.catalog();var extra=new CompoundTag();extra.putString("id","TEST_BOW");
        var enchants=new CompoundTag();
        enchants.putInt("aiming",5);enchants.putInt("impaling",1);enchants.putInt("flame",1);
        enchants.putInt("piercing",1);enchants.putInt("snipe",3);extra.put("enchantments",enchants);
        var result=new CraftCostCalculator(data,id->id.equals("TEST_BOW")?100.0:null).calculate(extra);
        assertEquals(100.0,result.coins());assertTrue(result.missing().isEmpty());
        enchants.putInt("snipe",4);
        assertNull(new CraftCostCalculator(data,id->id.equals("TEST_BOW")?100.0:null).calculate(extra).coins());
    }
    @Test void prestigeUsesAllPreviousStarsAndKeepsNonCoinMaterialsSeparate(){
        var data=JsonParser.parseString("""
            {"items":{"BASE":{"upgrade_costs":[[{"type":"ESSENCE","essence_type":"CRIMSON","amount":10},
                {"type":"ITEM","item_id":"HEAVY_PEARL","amount":2}]],
                "prestige":{"item_id":"HOT","costs":[{"type":"ITEM","item_id":"KUUDRA_TEETH","amount":3}]}},
                "HOT":{"upgrade_costs":[[{"type":"ESSENCE","essence_type":"CRIMSON","amount":20}]],
                "prestige":{"item_id":"FIERY","costs":[{"type":"ESSENCE","essence_type":"CRIMSON","amount":30}]}},
                "FIERY":{}}}
            """).getAsJsonObject();
        var extra=new CompoundTag();extra.putString("id","FIERY");
        var result=new CraftCostCalculator(data,id->switch(id){case "BASE"->100.0;case "ESSENCE_CRIMSON"->2.0;default->null;}).calculate(extra);
        assertEquals(220.0,result.coins());
        assertEquals(Map.of("KUUDRA_TEETH",3,"HEAVY_PEARL",2),result.materials());
        assertTrue(result.missing().isEmpty());
    }
    @Test void fieryAuroraNeedsNoAuctionListingForThePrestigedBase(){
        var extra=new CompoundTag();extra.putString("id","FIERY_AURORA_CHESTPLATE");
        extra.putString("modifier","loving");extra.putInt("rarity_upgrades",1);
        var attributes=new CompoundTag();attributes.putInt("mana_pool",1);extra.put("attributes",attributes);
        var enchants=new CompoundTag();enchants.putInt("ultimate_wisdom",5);enchants.putInt("growth",5);
        enchants.putInt("protection",5);enchants.putInt("rejuvenate",5);enchants.putInt("vampirism",5);
        extra.put("enchantments",enchants);
        var result=new CraftCostCalculator(CraftCostTooltip.catalog(),id->id.startsWith("FIERY_")||id.startsWith("HOT_")||id.startsWith("BURNING_")?null:10.0).calculate(extra);
        assertNotNull(result.coins());assertTrue(result.missing().isEmpty());
        assertTrue(result.materials().get("KUUDRA_TEETH")>0);
        assertTrue(result.materials().get("HEAVY_PEARL")>0);
    }
    @Test void realCatalogIncludesRecipesReforgesAndItemUpgrades(){
        var data=CraftCostTooltip.catalog();
        assertTrue(data.getAsJsonObject("recipes").size()>2000);
        var extra=new CompoundTag();extra.putString("id","ASPECT_OF_THE_END");
        var result=new CraftCostCalculator(data,id->switch(id){
            case "ENCHANTED_EYE_OF_ENDER"->10.0;case "ENCHANTED_DIAMOND"->20.0;default->null;
        }).calculate(extra);
        assertEquals(340.0,result.coins());
        assertTrue(data.getAsJsonObject("items").getAsJsonObject("HYPERION").has("upgrade_costs"));
        assertTrue(data.getAsJsonObject("reforges").has("ancient"));
    }

    @Test void rolledReforgesAreExcludedWithoutRequiringPricesOrFees(){
        var data=catalog();
        var rolled=new JsonObject();rolled.addProperty("type","blacksmith");
        var fees=new JsonObject();fees.addProperty("RARE",999999);
        rolled.add("costs",fees);data.getAsJsonObject("reforges").add("rolled",rolled);
        for(String modifier:List.of("blended","rolled")){
            var extra=item();extra.putString("modifier",modifier);
            var result=new CraftCostCalculator(data,id->id.equals("INGREDIENT")?10.0:null).calculate(extra);
            assertEquals(20.0,result.coins());
            assertTrue(result.missing().isEmpty());
        }
    }
    @Test void missingStonePriceStillMakesFullCostUnavailable(){
        var extra=item();extra.putString("modifier","ancient");
        var result=new CraftCostCalculator(catalog(),id->id.equals("INGREDIENT")?10.0:null).calculate(extra);
        assertNull(result.coins());assertTrue(result.missing().contains("STONE"));
    }
}