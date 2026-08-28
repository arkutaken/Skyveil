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

    @Test void recognizesAuctionHouseTitles() {
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("Auctions Browser"));
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("BIN Auction View"));
        assertTrue(PetMenuLevelOverlayRenderer.isAuctionTitle("Manage Auctions"));
        assertFalse(PetMenuLevelOverlayRenderer.isAuctionTitle("Bazaar"));
    }
}
