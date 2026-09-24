package name.skyveil.client.craftcost;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ArmorCraftCostTest {
    @TestFactory Stream<DynamicTest> everyKuudraSetSlotAndPrestigeTier(){
        var cases=new ArrayList<DynamicTest>();
        for(String set:List.of("AURORA","CRIMSON","TERROR","FERVOR","HOLLOW"))
            for(String slot:List.of("HELMET","CHESTPLATE","LEGGINGS","BOOTS"))
                for(String tier:List.of("","HOT_","BURNING_","FIERY_","INFERNAL_")){
                    String id=tier+set+"_"+slot,root=set+"_"+slot;
                    cases.add(DynamicTest.dynamicTest(id,()->{
                        var catalog=CraftCostTooltip.catalog();
                        assertTrue(catalog.getAsJsonObject("items").has(id),"Missing armor definition");
                        var requests=new HashSet<String>();
                        var calculator=new CraftCostCalculator(catalog,key->{
                            requests.add(key);
                            if(key.matches("(HOT_|BURNING_|FIERY_|INFERNAL_).+_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"))return null;
                            return 1.0;
                        });
                        var item=new CompoundTag();item.putString("id",id);
                        var baseline=calculator.calculate(item);
                        assertNotNull(baseline.coins(),()->"Base: "+baseline);
                        assertTrue(requests.contains(root),"Must include the original armor piece");
                        var upgraded=item.copy();
                        upgraded.putInt("hot_potato_count",15);upgraded.putInt("rarity_upgrades",1);
                        upgraded.putBoolean("artOfPeaceApplied",true);upgraded.putString("modifier","ancient");
                        var enchantments=new CompoundTag();enchantments.putInt("ultimate_wisdom",5);
                        enchantments.putInt("growth",5);enchantments.putInt("protection",5);
                        enchantments.putInt("rejuvenate",5);upgraded.put("enchantments",enchantments);
                        var definition=catalog.getAsJsonObject("items").getAsJsonObject(id);
                        int stars=definition.getAsJsonArray("upgrade_costs").size();
                        upgraded.putInt("upgrade_level",stars);
                        var gems=new CompoundTag();var unlocked=new ListTag();var indexes=new HashMap<String,Integer>();
                        for(var element:definition.getAsJsonArray("gemstone_slots")){
                            String type=element.getAsJsonObject().get("slot_type").getAsString();
                            int index=indexes.getOrDefault(type,0);indexes.put(type,index+1);
                            String key=type+"_"+index;
                            gems.putString(key,"PERFECT");
                            if(type.equals("COMBAT")||type.equals("UNIVERSAL")||type.equals("DEFENSIVE")||type.equals("OFFENSIVE"))
                                gems.putString(key+"_gem","SAPPHIRE");
                            unlocked.add(StringTag.valueOf(key));
                        }
                        gems.put("unlocked_slots",unlocked);upgraded.put("gems",gems);
                        var result=calculator.calculate(upgraded);
                        assertNotNull(result.coins(),()->"Upgraded: "+result);
                        assertTrue(result.missing().isEmpty());
                        assertTrue(result.coins()>baseline.coins());
                        assertTrue(requests.containsAll(Set.of("HOT_POTATO_BOOK","FUMING_POTATO_BOOK",
                            "RECOMBOBULATOR_3000","THE_ART_OF_PEACE","PRECURSOR_GEAR","ESSENCE_CRIMSON")));
                        if(!tier.isEmpty()){
                            assertTrue(result.materials().getOrDefault("KUUDRA_TEETH",0)>0);
                            assertTrue(result.materials().getOrDefault("HEAVY_PEARL",0)>0);
                        }
                    }));
                }
        return cases.stream();
    }

    @TestFactory Stream<DynamicTest> allCatalogArmorSlotsIncludeCommonAppliedUpgrades(){
        var catalog=CraftCostTooltip.catalog();
        return catalog.getAsJsonObject("items").keySet().stream()
            .filter(id->id.matches(".*_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"))
            .sorted().map(id->DynamicTest.dynamicTest(id,()->{
                var item=new CompoundTag();item.putString("id",id);
                var calculator=new CraftCostCalculator(catalog,key->1.0);
                var baseline=calculator.calculate(item);
                assertNotNull(baseline.coins(),()->"Base: "+baseline);
                item.putInt("hot_potato_count",15);item.putInt("rarity_upgrades",1);item.putBoolean("artOfPeaceApplied",true);
                var enchantments=new CompoundTag();enchantments.putInt("ultimate_wisdom",5);
                item.put("enchantments",enchantments);
                var result=calculator.calculate(item);
                assertNotNull(result.coins(),()->"Upgraded: "+result);
                assertEquals(baseline.coins()+18,result.coins(),0.00001);
                assertEquals(baseline.materials(),result.materials());
            }));
    }
}