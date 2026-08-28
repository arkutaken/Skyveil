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
}
