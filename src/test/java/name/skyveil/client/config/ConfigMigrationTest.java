package name.skyveil.client.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @Test void removedSystemsAreDroppedWithoutTouchingRetainedSettings(){
        var tree=JsonParser.parseString("""
            {
              "version": 14,
              "darkMode": "DARK",
              "compactDamage": true,
              "itemPrices": {"showAuction": false},
              "trophyFishing": {"pityOverlay": false},
              "baitSack": {"cachedBagCounts": {"WHALE_BAIT": 4}},
              "map": {"enabled": true, "minimapEnabled": true},
              "hunting": {"attributeProgress": false, "attributeSort": "PRICE"}
            }
            """);
        assertTrue(ConfigManager.migrateLegacyTree(tree));
        var root=tree.getAsJsonObject();
        assertFalse(root.has("itemPrices"));
        assertFalse(root.has("trophyFishing"));
        assertFalse(root.has("baitSack"));
        assertFalse(root.has("map"));
        assertEquals("DARK",root.get("darkMode").getAsString());
        assertTrue(root.get("compactDamage").getAsBoolean());
        assertFalse(root.getAsJsonObject("hunting").get("attributeProgress").getAsBoolean());
    }

    @Test void multipleChatCopyBindingsCollapseToTheFirstSingleBinding(){
        var tree=JsonParser.parseString("""
            {"chatCopy":{"enabled":true,"bindings":[
              {"mouseButton":0,"keys":[341,67]},
              {"mouseButton":2,"keys":[]}
            ]}}
            """);
        assertTrue(ConfigManager.migrateLegacyTree(tree));
        var chatCopy=tree.getAsJsonObject().getAsJsonObject("chatCopy");
        assertFalse(chatCopy.has("bindings"));
        assertEquals(0,chatCopy.getAsJsonObject("binding").get("mouseButton").getAsInt());
        assertEquals(2,chatCopy.getAsJsonObject("binding").getAsJsonArray("keys").size());
    }
    @Test void purpleStylesMigrateToDarkWithoutChangingOtherSettings(){
        var tree=JsonParser.parseString("""
            {"darkMode":"Darkish Purple","storagePreviewTheme":"DARK_PURPLE",
             "inventoryPreview":{"backgroundColor":"PURPLE","scale":1.5}}
            """);
        assertTrue(ConfigManager.migrateLegacyTree(tree));
        var root=tree.getAsJsonObject();
        assertEquals("DARK",root.get("darkMode").getAsString());
        assertEquals("DARK",root.get("storagePreviewTheme").getAsString());
        assertEquals("DARK",root.getAsJsonObject("inventoryPreview").get("backgroundColor").getAsString());
        assertEquals(1.5,root.getAsJsonObject("inventoryPreview").get("scale").getAsDouble());
        assertFalse(ConfigManager.migrateLegacyTree(tree));
    }
    @Test void malformedOptionalLegacyFieldsPreserveOtherSettings(){
        // A bad obsolete field should be repaired locally, never reset the whole config.
        for(String value:java.util.List.of("null","{}","[]")){
            var tree=JsonParser.parseString("{\"compactDamage\":true,\"inventoryPreview\":{\"backgroundColor\":"+value+"},\"chatCopy\":{\"binding\":{\"control\":"+value+",\"heldKey\":\"invalid\"}}}");
            assertTrue(ConfigManager.migrateLegacyTree(tree));
            var root=tree.getAsJsonObject();
            assertTrue(root.get("compactDamage").getAsBoolean());
            assertEquals("DARK",root.getAsJsonObject("inventoryPreview").get("backgroundColor").getAsString());
            assertTrue(root.getAsJsonObject("chatCopy").getAsJsonObject("binding").getAsJsonArray("keys").isEmpty());
            assertFalse(ConfigManager.migrateLegacyTree(tree));
        }
    }}
