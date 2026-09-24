package name.skyveil.client.gui;

import name.skyveil.client.config.SettingsRegistry;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HudSettingsNavigationTest {
    @Test void everyHudShortcutResolvesToItsExactSectionAndSetting(){
        SettingsRegistry.registerDefaults();
        var expected=Map.of(
            "hud.petDisplay.enabled","Pets > Pet Display",
            "hud.performance.enabled","Interface > Screen Overlays",
            "hud.inventoryPreview.enabled","Interface > Screen Overlays",
            "mining.crystalHollowsMap.enabled","Mining > Crystal Hollows Map",
            "mining.commissions.enabled","Mining > Commissions",
            "mining.pickaxeAbility.enabled","Mining > Pickaxe Ability",
            "hud.skillXp.enabled","Interface > Screen Overlays",
            "hud.playerStats.enabled","Interface > Screen Overlays");
        expected.forEach((key,path)->{
            var result=SearchManager.findSetting(key);
            assertNotNull(result,key);assertEquals(key,result.setting().key);assertEquals(path,result.path());
        });
        assertNull(SearchManager.findSetting("unknown.hud"));
    }
}
