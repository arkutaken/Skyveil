package name.skyveil.client.pet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import name.skyveil.client.itemrarity.SkyblockRarity;

/** Structured petInfo only: player-head profile UUIDs are never pet identities. */
record PetMetadata(String type,String uuid,String heldItem,SkyblockRarity tier,double xp,boolean active){
    // petInfo may be an object, a JSON-encoded string, or a field inside an outer
    // object. Invalid metadata returns unknown fields rather than a guessed pet.
    static PetMetadata parse(String input){
        try{
            var value=parseJson(input==null?"{}":input);
            if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString())value=JsonParser.parseString(value.getAsString());
            JsonObject json=value.getAsJsonObject();
            if(json.has("petInfo")){
                var info=json.get("petInfo");
                json=info.isJsonObject()?info.getAsJsonObject():JsonParser.parseString(info.getAsString()).getAsJsonObject();
            }
            double xp=json.has("exp")&&!json.get("exp").isJsonNull()?json.get("exp").getAsDouble():-1;
            return new PetMetadata(text(json,"type"),text(json,"uuid"),text(json,"heldItem"),
                SkyblockRarity.fromLabel(text(json,"tier")),Double.isFinite(xp)?xp:-1,
                json.has("active")&&!json.get("active").isJsonNull()&&json.get("active").getAsBoolean());
        }catch(RuntimeException ignored){return new PetMetadata("","","",null,-1,false);}
    }
    private static com.google.gson.JsonElement parseJson(String input){
        try{return JsonParser.parseString(input);}
        catch(com.google.gson.JsonParseException invalid){
            // Older translated containers escaped object keys without enclosing the object in a string.
            return JsonParser.parseString(input.replace("\\\"","\""));
        }
    }
    private static String text(JsonObject json,String key){
        var value=json.get(key);return value==null||value.isJsonNull()?"":value.getAsString().trim();
    }
}