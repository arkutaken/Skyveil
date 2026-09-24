package name.skyveil.client.mining;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorpseTypeTest {
    @Test void identifiesServerHelmetIds() {
        assertEquals(CorpseType.LAPIS, CorpseType.fromHelmet("LAPIS_ARMOR_HELMET"));
        assertEquals(CorpseType.TUNGSTEN, CorpseType.fromHelmet("MINERAL_HELMET"));
        assertEquals(CorpseType.UMBER, CorpseType.fromHelmet("ARMOR_OF_YOG_HELMET"));
        assertNull(CorpseType.fromHelmet("VANGUARD_HELMET"));
        assertNull(CorpseType.fromHelmet(""));
        assertNull(CorpseType.fromHelmet(null));
    }
    @Test void requiresLocationRatherThanChatOrCommissionMention() {
        assertTrue(CorpseType.isMiningArea("§7⏣ §bGlacite Mineshaft"));
        assertTrue(CorpseType.isMiningArea("Area: Glacite Mineshafts"));
        assertTrue(CorpseType.isMiningArea("⏣ Base Camp"));
        assertTrue(CorpseType.isMiningArea("⏣ Glacite Tunnels"));
        assertFalse(CorpseType.isMiningArea("Mineshaft Explorer: 50%"));
        assertFalse(CorpseType.isMiningArea("⏣ Your Island"));
        assertFalse(CorpseType.isMiningArea("Visit Base Camp"));
    }
}
