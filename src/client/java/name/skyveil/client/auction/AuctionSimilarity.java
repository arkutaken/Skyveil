package name.skyveil.client.auction;

import com.google.gson.*;
import net.minecraft.nbt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Exact upgrade compatibility; never substitutes an unmodified item for an upgraded one. */
final class AuctionSimilarity {
    private static final List<String> FIELDS=List.of(
        "modifier","enchantments","runes","attributes","gems","ability_scroll","power_ability_scroll",
        "rarity_upgrades","hot_potato_count","art_of_war_count","farming_for_dummies_count",
        "wood_singularity_count","tuned_transmission","mana_disintegrator_count","jalapeno_count",
        "polarvoid","ethermerge","drill_part_engine","drill_part_fuel_tank","drill_part_upgrade_module",
        "dye_item","skin","dungeon_item","baseStatBoostPercentage","item_tier","item_durability",
        "new_years_cake","edition","model","color");
    private AuctionSimilarity(){}
    /**
     * Canonicalizes supported value-affecting metadata before hashing it.
     * Equivalent field order and legacy aliases must produce the same key.
     * An empty key means the item cannot safely participate in comparisons.
     */
    static String key(CompoundTag extra){
        String identity=AuctionPrices.identity(extra);
        if(identity.isBlank())return "";
        var parts=new TreeMap<String,JsonElement>();
        parts.put("id",new JsonPrimitive(identity));
        for(String field:FIELDS){
            var value=field.equals("enchantments")?name.skyveil.client.craftcost.CraftCostTooltip.paidEnchantments(extra):extra.get(field);
            if(value!=null){var normalized=canonical(value);if(normalized!=null)parts.put(field,normalized);}
        }
        addNumber(parts,"stars",extra.getIntOr("upgrade_level",extra.getIntOr("dungeon_item_level",0)));
        addNumber(parts,"art_of_peace",Math.max(extra.getIntOr("art_of_peace_count",0),extra.getBooleanOr("artOfPeaceApplied",false)?1:0));
        addNumber(parts,"stats_book",Math.max(extra.getIntOr("book_of_stats",0),extra.getBooleanOr("stats_book",false)?1:0));
        if(extra.getStringOr("id","").equalsIgnoreCase("PET")){
            try{
                String raw=extra.getStringOr("petInfo","");
                JsonElement parsed;
                try{parsed=JsonParser.parseString(raw);}catch(JsonParseException error){parsed=JsonParser.parseString(raw.replace("\\\"","\""));}
                if(parsed.isJsonPrimitive())parsed=JsonParser.parseString(parsed.getAsString());
                var pet=parsed.getAsJsonObject();var comparable=new TreeMap<String,JsonElement>();
                for(String field:List.of("skin")){
                    var value=pet.get(field);
                    if(value!=null&&!value.isJsonNull())comparable.put(field,value);
                }
                parts.put("pet",new Gson().toJsonTree(comparable));
            }catch(RuntimeException invalid){return "";}
        }
        try{
            byte[] bytes=new Gson().toJson(parts).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static void addNumber(Map<String,JsonElement> parts,String key,int value){
        if(value!=0)parts.put(key,new JsonPrimitive(value));
    }
    // Sort compound keys and list entries so NBT storage order does not change
    // compatibility. Empty and zero values are normalized to absence.
    private static JsonElement canonical(Tag value){
        if(value instanceof CompoundTag compound){
            var result=new TreeMap<String,JsonElement>();
            for(String key:compound.keySet()){
                // Gem UUIDs identify the individual gem, not its type or quality.
                if(key.equalsIgnoreCase("uuid")||key.endsWith("_uuid"))continue;
                var entry=canonical(compound.get(key));if(entry!=null)result.put(key,entry);
            }
            return result.isEmpty()?null:new Gson().toJsonTree(result);
        }
        if(value instanceof ListTag list){
            var values=new ArrayList<String>();
            for(Tag entry:list){var normalized=canonical(entry);if(normalized!=null)values.add(normalized.toString());}
            Collections.sort(values);var result=new JsonArray();
            for(String entry:values)result.add(JsonParser.parseString(entry));
            return result.isEmpty()?null:result;
        }
        if(value instanceof NumericTag){
            double number=value.asDouble().orElse(0.0);
            return number==0?null:new JsonPrimitive(number);
        }
        String text=value.asString().orElse(value.toString()).trim().toUpperCase(Locale.ROOT);
        return text.isEmpty()?null:new JsonPrimitive(text);
    }
}
