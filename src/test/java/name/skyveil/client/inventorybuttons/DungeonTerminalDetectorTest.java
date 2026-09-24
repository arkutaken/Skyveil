package name.skyveil.client.inventorybuttons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonTerminalDetectorTest {
    @Test
    void recognizesF7AndM7TerminalTitles() {
        assertTrue(DungeonTerminalDetector.matchesTitle("Correct all the panes!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Navigate the maze!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Click in order!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("What starts with: 'A'?"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Select all the RED items!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Change all to same color!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Click the button on time!"));
        assertTrue(DungeonTerminalDetector.matchesTitle("Align all the arrows!"));
    }

    @Test
    void keepsButtonsAvailableInOrdinarySkyBlockContainers() {
        assertFalse(DungeonTerminalDetector.matchesTitle("Auction House"));
        assertFalse(DungeonTerminalDetector.matchesTitle("Bazaar"));
        assertFalse(DungeonTerminalDetector.matchesTitle("Storage"));
        assertFalse(DungeonTerminalDetector.matchesTitle("Inventory"));
    }
}
