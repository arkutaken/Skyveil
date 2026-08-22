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
              "hunting": {"attributeProgress": false, "attributeSort": "PRICE"}
            }
            """);
        assertTrue(ConfigManager.migrateLegacyTree(tree));
        var root=tree.getAsJsonObject();
        assertFalse(root.has("itemPrices"));
        assertFalse(root.has("trophyFishing"));
        assertFalse(root.has("baitSack"));
        assertEquals("DARK",root.get("darkMode").getAsString());
        assertTrue(root.get("compactDamage").getAsBoolean());
        assertFalse(root.getAsJsonObject("hunting").get("attributeProgress").getAsBoolean());
    }
}
