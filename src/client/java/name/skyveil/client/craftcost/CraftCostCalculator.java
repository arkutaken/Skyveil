package name.skyveil.client.craftcost;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.function.Function;

/** Replacement ingredient cost, never a resale appraisal or a partial sum labelled as complete. */
public final class CraftCostCalculator {
    private int remaining;
    private final JsonObject catalog;
    private final Function<String,Double> prices;
    public CraftCostCalculator(JsonObject catalog,Function<String,Double> prices){this.catalog=catalog;this.prices=prices;}
    public record Result(Double coins,Set<String> missing,Map<String,Integer> materials){
        public Result(Double coins,Set<String> missing){this(coins,missing,Map.of());}
    }
    public Result calculate(CompoundTag extra){
        remaining=2048;
        var sum=new Sum();
        String id=extra.getStringOr("id","");
        if(id.isEmpty())return new Result(null,Set.of("Item ID"));
        String baseId=Set.of("PET","RUNE","ENCHANTED_BOOK").contains(id)?name.skyveil.client.auction.AuctionPrices.identity(extra):id;
        // A prestige consumes the previous tier at its maximum star level.
        var visitedTiers=new HashSet<String>();
        var definitions=object(catalog,"items");
        while(visitedTiers.add(baseId)){
            String parentId=null;JsonObject parent=null;
            for(var entry:definitions.entrySet()){
                var candidate=entry.getValue().getAsJsonObject();
                if(text(object(candidate,"prestige"),"item_id").equals(baseId)){
                    if(parentId!=null){sum.missing.add("Ambiguous prestige recipe");break;}
                    parentId=entry.getKey();parent=candidate;
                }
            }
            if(parentId==null)break;
            var prestige=object(parent,"prestige");
            if(prestige.has("costs"))sum.costs(prestige.getAsJsonArray("costs"));
            if(parent.has("upgrade_costs"))for(var level:parent.getAsJsonArray("upgrade_costs"))sum.costs(level.getAsJsonArray());
            baseId=parentId;
        }
        sum.add(base(baseId,new HashSet<>(),0),baseId);
        var definition=object(object(catalog,"items"),id);
        if(!id.equals("ENCHANTED_BOOK")){
            var enchants=extra.getCompoundOrEmpty("enchantments");
            for(String enchant:enchants.keySet()){
                int level=enchants.getIntOr(enchant,0);
                                if(enchant.equals("efficiency")&&level>5){
                    int innate=id.equals("STONK")?6:5;
                    sum.item("SILEX",Math.max(0,level-innate));
                }else if(level>0)sum.add(enchant(enchant.toUpperCase(Locale.ROOT),level),"Enchantment "+enchant+" "+level);
            }
        }
        if(!id.equals("RUNE")){
            var runes=extra.getCompoundOrEmpty("runes");
            for(String rune:runes.keySet())sum.item("RUNE:"+rune.toUpperCase(Locale.ROOT)+":"+runes.getIntOr(rune,0),1);
        }
        int potatoes=extra.getIntOr("hot_potato_count",0);
        sum.item("HOT_POTATO_BOOK",Math.min(10,potatoes));
        sum.item("FUMING_POTATO_BOOK",Math.max(0,potatoes-10));
        sum.item("RECOMBOBULATOR_3000",extra.getIntOr("rarity_upgrades",0));
        Map<String,String> upgrades=Map.ofEntries(
            Map.entry("art_of_war_count","THE_ART_OF_WAR"),Map.entry("art_of_peace_count","THE_ART_OF_PEACE"),
            Map.entry("farming_for_dummies_count","FARMING_FOR_DUMMIES"),Map.entry("wood_singularity_count","WOOD_SINGULARITY"),
            Map.entry("tuned_transmission","TRANSMISSION_TUNER"),Map.entry("mana_disintegrator_count","MANA_DISINTEGRATOR"),
            Map.entry("jalapeno_count","JALAPENO_BOOK"),Map.entry("polarvoid","POLARVOID_BOOK"),
            Map.entry("book_of_stats","BOOK_OF_STATS"));
        upgrades.forEach((key,product)->sum.item(product,extra.getIntOr(key,0)));
                if(extra.getBooleanOr("artOfPeaceApplied",false)&&extra.getIntOr("art_of_peace_count",0)==0)sum.item("THE_ART_OF_PEACE",1);
        if(extra.getBooleanOr("stats_book",false)&&extra.getIntOr("book_of_stats",0)==0)sum.item("BOOK_OF_STATS",1);
        if(extra.getBooleanOr("ethermerge",false)){sum.item("ETHERWARP_CONDUIT",1);sum.item("ETHERWARP_MERGER",1);}
        for(var scroll:extra.getListOrEmpty("ability_scroll"))scroll.asString().ifPresent(value->sum.item(value,1));
        String powerScroll=extra.getStringOr("power_ability_scroll","");
        if(!powerScroll.isEmpty())sum.item(powerScroll.toUpperCase(Locale.ROOT).endsWith("_POWER_SCROLL")?powerScroll.toUpperCase(Locale.ROOT):powerScroll.toUpperCase(Locale.ROOT)+"_POWER_SCROLL",1);

        String modifier=extra.getStringOr("modifier","");
        if(!modifier.isEmpty()){
            var reforge=reforge(modifier);
            // Only stone applications are craft inputs. Blacksmith rolls (including
            // modifiers absent from the stone catalog) do not add costs or missing inputs.
            if(text(reforge,"type").contains("reforge_stone")){
                sum.item(text(reforge,"item"),1);
                String tier=text(definition,"tier");
                if(extra.getIntOr("rarity_upgrades",0)>0){
                    var tiers=List.of("COMMON","UNCOMMON","RARE","EPIC","LEGENDARY","MYTHIC","DIVINE");
                    int index=tiers.indexOf(tier);if(index>=0&&index<tiers.size()-1)tier=tiers.get(index+1);
                }
                var costs=object(reforge,"costs");
                sum.add(costs.has(tier)?costs.get(tier).getAsDouble():null,"Reforge fee");
            }
        }
        int stars=extra.getIntOr("upgrade_level",extra.getIntOr("dungeon_item_level",0));
        var costs=definition.has("upgrade_costs")?definition.getAsJsonArray("upgrade_costs"):new JsonArray();
        for(int star=0;star<Math.min(stars,costs.size());star++)sum.costs(costs.get(star).getAsJsonArray());
        if(stars>costs.size()){
            String[] master={"FIRST_MASTER_STAR","SECOND_MASTER_STAR","THIRD_MASTER_STAR","FOURTH_MASTER_STAR","FIFTH_MASTER_STAR"};
            if(costs.size()==5&&stars<=10)for(int star=5;star<stars;star++)sum.item(master[star-5],1);
            else sum.missing.add("Star upgrade costs");
        }
        if(extra.getIntOr("dungeon_item",0)>0&&definition.has("dungeon_item_conversion_cost")){
            var conversion=definition.get("dungeon_item_conversion_cost");
            if(conversion.isJsonArray())sum.costs(conversion.getAsJsonArray());else sum.cost(conversion.getAsJsonObject());
        }
        var gems=extra.getCompoundOrEmpty("gems");
        for(String slot:gems.keySet()){
            if(slot.equals("unlocked_slots")||slot.endsWith("_gem"))continue;
            var value=gems.get(slot);
            String quality=value.asString().orElseGet(()->value.asCompound().map(c->c.getStringOr("quality","")).orElse(""));
            if(quality.isEmpty())continue;
            String kind=gems.getStringOr(slot+"_gem",slot.replaceFirst("_\\d+$",""));
            if(!Set.of("RUBY","AMETHYST","SAPPHIRE","JADE","AMBER","TOPAZ","JASPER","OPAL","AQUAMARINE","CITRINE","ONYX","PERIDOT").contains(kind)){
                sum.missing.add("Gemstone "+slot);continue;
            }
            sum.item(quality+"_"+kind+"_GEM",1);
        }
        Set<String> unlocked=new HashSet<>();
        for(var slot:gems.getListOrEmpty("unlocked_slots"))slot.asString().ifPresent(unlocked::add);
        for(String slot:gems.keySet())if(!slot.endsWith("_gem")&&!slot.equals("unlocked_slots"))unlocked.add(slot);
        var slots=definition.has("gemstone_slots")?definition.getAsJsonArray("gemstone_slots"):new JsonArray();
        Map<String,Integer> indices=new HashMap<>();
        for(var element:slots){
            var slot=element.getAsJsonObject();String type=text(slot,"slot_type");
            int index=indices.getOrDefault(type,0);indices.put(type,index+1);
            if(unlocked.remove(type+"_"+index)&&slot.has("costs"))sum.costs(slot.getAsJsonArray("costs"));
        }
        if(!unlocked.isEmpty())sum.missing.add("Gemstone slot unlocks");
        // These upgrades are item-specific; never silently pretend they are free.
        for(String key:List.of("attributes","drill_part_engine","drill_part_fuel_tank","drill_part_upgrade_module","dye_item","skin")){
            if(!extra.contains(key))continue;
            if(key.equals("attributes")){if(!extra.getCompoundOrEmpty(key).isEmpty()&&!name.skyveil.client.auction.AuctionPrices.hasOnlyInnateKuudraAttributes(extra))sum.missing.add("Attribute upgrades");}
            else{String product=extra.getStringOr(key,"").trim().toUpperCase(Locale.ROOT);if(!product.isEmpty())sum.item(product,1);}
        }
        return new Result(sum.missing.isEmpty()&&Double.isFinite(sum.total)?sum.total:null,Set.copyOf(sum.missing),Map.copyOf(sum.materials));
    }

    /** Server modifier IDs can split words differently from the displayed reforge name. */
    private JsonObject reforge(String modifier){
        var reforges=object(catalog,"reforges");
        var exact=object(reforges,modifier);
        if(!exact.isEmpty())return exact;
        String normalized=normalizeReforge(modifier);
        JsonObject match=null;
        for(var entry:reforges.entrySet()){
            if(!normalizeReforge(entry.getKey()).equals(normalized)
                &&!normalizeReforge(text(entry.getValue().getAsJsonObject(),"item")).equals(normalized))continue;
            if(match!=null)return new JsonObject(); // Do not price an ambiguous alias.
            match=entry.getValue().getAsJsonObject();
        }
        return match==null?new JsonObject():match;
    }
    private static String normalizeReforge(String name){
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    }

    private Double enchant(String name,int level){
        if(level>20)return null;
        var table=object(catalog,"tableEnchants");
        if(table.has(name)&&level<=table.get(name).getAsInt())return 0.0;
        if(Set.of("EXPERTISE","COMPACT","CULTIVATING","CHAMPION","HECATOMB","TOXOPHILITE").contains(name))
            return prices.apply("ENCHANTMENT_"+name+"_1");
        Double best=prices.apply("ENCHANTMENT_"+name+"_"+level);
        for(int lower=level-1;lower>=1;lower--){
            Double price=prices.apply("ENCHANTMENT_"+name+"_"+lower);
            if(price!=null){double cost=price*(1L<<(level-lower));if(best==null||cost<best)best=cost;}
        }
        return best;
    }

    private Double base(String id,Set<String> visiting,int depth){
        if(--remaining<0||depth>20||!visiting.add(id))return null;
        try{
            var recipes=object(catalog,"recipes").get(id);
            if(recipes==null||!recipes.isJsonArray())return prices.apply(id);
            Double best=null;
            for(var element:recipes.getAsJsonArray()){
                var recipe=element.getAsJsonObject();double total=0;boolean complete=true;
                for(var ingredient:object(recipe,"ingredients").entrySet()){
                    String key=ingredient.getKey();
                    Double cost=key.equals("SKYBLOCK_COIN")?Double.valueOf(1.0):prices.apply(key);
                    if(cost==null)cost=base(key,visiting,depth+1);
                    if(cost==null){complete=false;break;}
                    total+=cost*ingredient.getValue().getAsDouble();
                }
                double count=recipe.get("count").getAsDouble();
                if(complete&&count>0&&Double.isFinite(total)&&(best==null||total/count<best))best=total/count;
            }
            return best==null?prices.apply(id):best;
        }finally{visiting.remove(id);}
    }

    private final class Sum{
        double total;final Set<String> missing=new TreeSet<>();final Map<String,Integer> materials=new TreeMap<>();
        void add(Double value,String label){if(value==null||!Double.isFinite(value)||value<0)missing.add(label);else total+=value;}
                void item(String id,int count){
            if(count<=0)return;
            if(id.equals("KUUDRA_TEETH")||id.equals("HEAVY_PEARL")){
                materials.merge(id,count,Integer::sum);return;
            }
            Double price=prices.apply(id);add(price==null?null:price*count,id);
        }
        void costs(JsonArray costs){for(var cost:costs)cost(cost.getAsJsonObject());}
        void cost(JsonObject cost){
            switch(text(cost,"type")){
                case "COINS"->add(cost.get("coins").getAsDouble(),"Coins");
                case "ITEM"->item(text(cost,"item_id"),cost.get("amount").getAsInt());
                case "ESSENCE"->item("ESSENCE_"+text(cost,"essence_type"),cost.get("amount").getAsInt());
                default->missing.add("Unknown upgrade cost");
            }
        }
    }
    private static JsonObject object(JsonObject parent,String key){var value=parent.get(key);return value!=null&&value.isJsonObject()?value.getAsJsonObject():new JsonObject();}
    private static String text(JsonObject object,String key){var value=object.get(key);return value==null?"":value.getAsString();}
}