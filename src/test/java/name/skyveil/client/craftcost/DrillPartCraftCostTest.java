package name.skyveil.client.craftcost;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DrillPartCraftCostTest {
    private static CompoundTag drill(){
        var item=new CompoundTag();item.putString("id","TITANIUM_DRILL_4");return item;
    }
    @Test void installedLowercasePartsUseMarketIdsAndAreChargedExactlyOnce(){
        var item=drill();
        item.putString("drill_part_fuel_tank","gemstone_fuel_tank");
        item.putString("drill_part_engine","ruby_polished_drill_engine");
        item.putString("drill_part_upgrade_module","goblin_omelette_spicy");
        var prices=Map.of("TITANIUM_DRILL_4",100.0,"GEMSTONE_FUEL_TANK",20.0,
            "RUBY_POLISHED_DRILL_ENGINE",30.0,"GOBLIN_OMELETTE_SPICY",40.0);
        var requested=new ArrayList<String>();
        var result=new CraftCostCalculator(new JsonObject(),id->{requested.add(id);return prices.get(id);}).calculate(item);
        assertEquals(190.0,result.coins());assertTrue(result.missing().isEmpty());
        for(String part:prices.keySet())assertEquals(1,Collections.frequency(requested,part));
    }
    @Test void absentOrEmptySlotsNeverRequestOrChargeParts(){
        var item=drill();var requested=new ArrayList<String>();
        var calculator=new CraftCostCalculator(new JsonObject(),id->{requested.add(id);return id.equals("TITANIUM_DRILL_4")?100.0:null;});
        assertEquals(100.0,calculator.calculate(item).coins());
        assertEquals(List.of("TITANIUM_DRILL_4"),requested);
        requested.clear();
        item.putString("drill_part_engine","");
        item.putString("drill_part_fuel_tank"," ");
        item.putString("drill_part_upgrade_module","");
        assertEquals(100.0,calculator.calculate(item).coins());
        assertEquals(List.of("TITANIUM_DRILL_4"),requested);
    }
    @Test void oneInstalledPartDoesNotImplyOtherParts(){
        var item=drill();item.putString("drill_part_engine","ruby_polished_drill_engine");
        var requested=new ArrayList<String>();
        var result=new CraftCostCalculator(new JsonObject(),id->{
            requested.add(id);return id.equals("TITANIUM_DRILL_4")?100.0:null;
        }).calculate(item);
        assertNull(result.coins());
        assertEquals(Set.of("RUBY_POLISHED_DRILL_ENGINE"),result.missing());
        assertEquals(List.of("TITANIUM_DRILL_4","RUBY_POLISHED_DRILL_ENGINE"),requested);
    }
    @Test void installedPartsFallBackToRecipesWhenDirectListingsAreMissing(){
        // Only the installed tank is reconstructed; absent engine/module slots add nothing.
        var catalog=com.google.gson.JsonParser.parseString("""
            {"recipes":{"GEMSTONE_FUEL_TANK":[{"count":1,"ingredients":{"TITANIUM_FUEL_TANK":1,"GEMSTONE_MIXTURE":10}}],
            "TITANIUM_FUEL_TANK":[{"count":1,"ingredients":{"REFINED_TITANIUM":10}}]}}
            """).getAsJsonObject();
        var item=drill();item.putString("drill_part_fuel_tank","gemstone_fuel_tank");
        var prices=Map.of("TITANIUM_DRILL_4",100.0,"GEMSTONE_MIXTURE",2.0,"REFINED_TITANIUM",3.0);
        var result=new CraftCostCalculator(catalog,prices::get).calculate(item);
        assertEquals(150.0,result.coins());
        assertTrue(result.missing().isEmpty());
        assertTrue(new CraftCostVisibility(CraftCostTooltip.catalog()).shouldShow(item));
    }
    @Test void missingRecipeIngredientsStillLeaveFullCostUnavailable(){
        var catalog=com.google.gson.JsonParser.parseString("""
            {"recipes":{"GEMSTONE_FUEL_TANK":[{"count":1,"ingredients":{"UNKNOWN_INPUT":1}}]}}
            """).getAsJsonObject();
        var item=drill();item.putString("drill_part_fuel_tank","gemstone_fuel_tank");
        var result=new CraftCostCalculator(catalog,id->id.equals("TITANIUM_DRILL_4")?100.0:null).calculate(item);
        assertNull(result.coins());
        assertEquals(Set.of("GEMSTONE_FUEL_TANK"),result.missing());
    }
}
