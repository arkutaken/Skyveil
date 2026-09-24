package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetLevelParserTest {
    @Test void parsesPetMenuLevels() {
        assertEquals(100,PetLevelParser.parse("[Lvl 100] Griffin").level());
        assertEquals(200,PetLevelParser.parse("\u00a76[Lvl 200] Golden Dragon").level());
    }

    @Test void rejectsNonPetMenuLabels() {
        assertNull(PetLevelParser.parse("Griffin"));
        assertNull(PetLevelParser.parse("[Lvl 0] Griffin"));
        assertNull(PetLevelParser.parse("[Lvl 100]"));
    }

    @Test void parsesCompactActivePetWidgetLayouts() {
        assertEquals(new PetLevelParser.Parsed(86,"Lion"),PetLevelParser.parseWidget("Lvl 86 Lion"));
        assertEquals(new PetLevelParser.Parsed(100,"Golden Dragon"),PetLevelParser.parseWidget("Golden Dragon (Lvl 100)"));
        assertEquals(new PetLevelParser.Parsed(200,"Golden Dragon"),PetLevelParser.parseWidget("[Lvl 200] Golden Dragon"));
    }

    @Test void recognizesAuctionHouseTitles() {
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("Auctions Browser"));
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("BIN Auction View"));
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("Manage Auctions"));
        assertFalse(PetMenuLevelOverlayRenderer.isAuctionTitle("Bazaar"));
    }
}
