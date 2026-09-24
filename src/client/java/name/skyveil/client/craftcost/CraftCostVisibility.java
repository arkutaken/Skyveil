package name.skyveil.client.craftcost;

import com.google.gson.JsonObject;
import name.skyveil.client.auction.AuctionPrices;
import net.minecraft.nbt.CompoundTag;
import java.util.HashSet;
import java.util.Set;

/** Hide raw, noncraftable ingredients without excluding crafted or upgraded items. */
final class CraftCostVisibility {
    private final Set<String> crafted=new HashSet<>(),ingredients=new HashSet<>();
    CraftCostVisibility(JsonObject catalog){
        var recipes=catalog.getAsJsonObject("recipes");
        if(recipes!=null)for(var entry:recipes.entrySet()){
            if(!entry.getValue().isJsonArray())continue;
            for(var recipe:entry.getValue().getAsJsonArray()){
                if(!recipe.isJsonObject())continue;
                var inputs=recipe.getAsJsonObject().getAsJsonObject("ingredients");
                if(inputs==null||inputs.isEmpty())continue;
                crafted.add(entry.getKey());ingredients.addAll(inputs.keySet());
            }
        }
        // Prestige transformations are recipes too, represented separately in the catalog.
        var items=catalog.getAsJsonObject("items");
        if(items!=null)for(var entry:items.entrySet()){
            var prestige=entry.getValue().getAsJsonObject().getAsJsonObject("prestige");
            if(prestige!=null&&prestige.has("item_id")){
                crafted.add(prestige.get("item_id").getAsString());
                ingredients.add(entry.getKey());
            }
        }
    }
    boolean shouldShow(CompoundTag extra){
        String id=extra.getStringOr("id","");
        return crafted.contains(id)||!ingredients.contains(id)
            ||!AuctionPrices.isUnmodified(extra)||extra.getIntOr("dungeon_item",0)>0;
    }
}
