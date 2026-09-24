package name.skyveil.client.mining;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PickaxeAbilityParserTest {
    @Test void inlineReadyAndSeconds(){
        assertTrue(PickaxeAbilityParser.parse(List.of("§6Pickaxe Ability: §aAvailable")).ready());
        assertEquals("42s",PickaxeAbilityParser.parse(List.of("Pickaxe Ability: 42s")).status());
    }
    @Test void namedAbilityWithSeparateCooldown(){
        var value=PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","Mining Speed Boost","Cooldown: 1m 20s","Commissions:"));
        assertEquals("Mining Speed Boost",value.ability());assertEquals("1m 20s",value.status());assertFalse(value.ready());
        assertEquals("1:20",PickaxeAbilityParser.parse(List.of("Pickaxe Ability: Mining Speed Boost","1:20")).status());
    }
    @Test void unrelatedOrMissingWidgetNeverShowsStaleOrInventedReadiness(){
        assertNull(PickaxeAbilityParser.parse(List.of("Other Ability: 10s")));
        assertNull(PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","","Cooldown: 20s")));
        assertNull(PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","Commissions:","Miner: 20%")));
        assertNull(PickaxeAbilityParser.parse(List.of("Pickaxe Ability: nonsense")));
    }

    @Test void namedStatusOnOneWidgetRow(){
        var value=PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","Mining Speed Boost: 42s"));
        assertNotNull(value);assertEquals("Mining Speed Boost",value.ability());assertEquals("42s",value.status());
        assertTrue(PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","Pickobulus: Ready!")).ready());
        assertEquals("1m 20s",PickaxeAbilityParser.parse(List.of("Pickaxe Ability: Mining Speed Boost (1m 20s)")).status());
        assertEquals("Active",PickaxeAbilityParser.parse(List.of("Pickaxe Ability:","Gemstone Infusion - Active")).status());
    }
    @Test void formattedSpacingAndAvailableIn(){
        var value=PickaxeAbilityParser.parse(List.of("\u00a76Pickaxe Ability Cooldowns:","\u00a0\u00a7ePickobulus: Available in 37s\u00a0"));
        assertNotNull(value);assertEquals("37s",value.status());assertFalse(value.ready());
    }
}
