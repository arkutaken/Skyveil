package name.skyveil.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Repairs legacy JSON before binding, independently of configuration I/O and validation. */
final class ConfigMigration {
    private ConfigMigration(){}
    /** Converts legacy JSON shapes before Gson binds them to the current strongly typed model. */
    static boolean migrate(JsonElement tree){
        if(tree==null||!tree.isJsonObject())return false;
        JsonObject root=tree.getAsJsonObject();
        // Evaluate every migration, even when an earlier one changed the tree.
        return migrateStyles(root)|removeRetiredFeatures(root)|migrateSlotLinks(root)|migrateRarity(root)|migrateChatCopy(root);
    }

    /** Retire removed visual styles while preserving unrelated settings. */
    private static boolean migrateStyles(JsonObject root){
        boolean changed=false;
        // Rewrite retired styles before binding JSON, and persist the migration once.
        for(String key:java.util.List.of("darkMode","storagePreviewTheme")){
            var value=root.get(key);
            if(value!=null&&value.isJsonPrimitive()&&value.getAsString().toUpperCase(java.util.Locale.ROOT).contains("PURPLE")){
                root.addProperty(key,"DARK");changed=true;
            }
        }
        if(root.has("inventoryPreview")&&root.get("inventoryPreview").isJsonObject()){
            var preview=root.getAsJsonObject("inventoryPreview");
            if(preview.has("backgroundColor")&&(!preview.get("backgroundColor").isJsonPrimitive()||!"DARK".equals(preview.get("backgroundColor").getAsString()))){
                preview.addProperty("backgroundColor","DARK");changed=true;
            }
        }

        return changed;
    }

    /** Discard only keys belonging to removed modules. */
    private static boolean removeRetiredFeatures(JsonObject root){
        boolean changed=false;
        for(String key:new String[]{"trophyFishing","bobberTimer","baitSack","fishingNavigation","seaCreatures","itemPrices","trophyDiamondCaught","trophyTierCounts","trophyTotalCounts","map"})
            if(root.remove(key)!=null)changed=true;
        return changed;
    }

    /** Promote the old single destination to the current destination list. */
    private static boolean migrateSlotLinks(JsonObject root){
        boolean changed=false;
        JsonElement protectionElement=root.get("itemProtection");
        if(protectionElement!=null&&protectionElement.isJsonObject()){
            JsonElement linksElement=protectionElement.getAsJsonObject().get("slotLinks");
            if(linksElement!=null&&linksElement.isJsonObject())for(var entry:linksElement.getAsJsonObject().entrySet())
                if(entry.getValue()!=null&&entry.getValue().isJsonPrimitive()){
                    com.google.gson.JsonArray destinations=new com.google.gson.JsonArray();destinations.add(entry.getValue());entry.setValue(destinations);changed=true;
                }
        }
        return changed;
    }

    /** Collapse per-rarity switches into the current master switch. */
    private static boolean migrateRarity(JsonObject root){
        boolean changed=false;
        JsonElement rarityElement=root.get("itemRarity");
        if(rarityElement!=null&&rarityElement.isJsonObject()){
            JsonObject rarity=rarityElement.getAsJsonObject();
            String[] legacyFlags={"common","uncommon","rare","epic","legendary","mythic","divine","special","verySpecial","supreme","ultimate","admin"};
            if(!rarity.has("enabled")){
                boolean enabled=false,found=false;
                for(String flag:legacyFlags)if(rarity.has(flag)&&rarity.get(flag).isJsonPrimitive()){
                    found=true;try{enabled|=rarity.get(flag).getAsBoolean();}catch(Exception ignored){}
                }
                rarity.addProperty("enabled",!found||enabled);changed=true;
            }
            String[] dead={"style","opacity","outlineOpacity","outlineThickness","common","uncommon","rare","epic","legendary","mythic","divine","special","verySpecial","supreme","ultimate","admin"};
            for(String key:dead)if(rarity.remove(key)!=null)changed=true;
        }
        return changed;
    }

    /** Retain the first binding when migrating from the old multi-binding shape. */
    private static boolean migrateChatCopy(JsonObject root){
        boolean changed=false;
        JsonElement chatCopyElement=root.get("chatCopy");
        if(chatCopyElement!=null&&chatCopyElement.isJsonObject()){
            JsonObject chatCopy=chatCopyElement.getAsJsonObject();JsonObject binding=null;JsonElement current=chatCopy.get("binding");if(current!=null&&current.isJsonObject())binding=current.getAsJsonObject();
            JsonElement bindings=chatCopy.get("bindings");if(binding==null&&bindings!=null&&bindings.isJsonArray()&&!bindings.getAsJsonArray().isEmpty()&&bindings.getAsJsonArray().get(0).isJsonObject()){binding=bindings.getAsJsonArray().get(0).getAsJsonObject().deepCopy();chatCopy.add("binding",binding);changed=true;}
            if(chatCopy.remove("bindings")!=null)changed=true;
            if(binding!=null&&!binding.has("keys")){com.google.gson.JsonArray keys=new com.google.gson.JsonArray();
                if(booleanValue(binding,"control"))keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL);
                if(booleanValue(binding,"shift"))keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT);
                if(booleanValue(binding,"alt"))keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT);
                if(binding.has("heldKey")){int key=integerValue(binding,"heldKey",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);if(key!=org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN)keys.add(key);}binding.add("keys",keys);binding.remove("control");binding.remove("shift");binding.remove("alt");binding.remove("heldKey");changed=true;
            }
        }
        return changed;
    }

    // Bad optional legacy fields must not discard the rest of a user's settings.
    private static boolean booleanValue(JsonObject object,String key){
        JsonElement value=object.get(key);
        return value!=null&&value.isJsonPrimitive()&&value.getAsBoolean();
    }

    private static int integerValue(JsonObject object,String key,int fallback){
        JsonElement value=object.get(key);
        if(value==null||!value.isJsonPrimitive())return fallback;
        try{return value.getAsInt();}catch(NumberFormatException invalid){return fallback;}
    }
}
