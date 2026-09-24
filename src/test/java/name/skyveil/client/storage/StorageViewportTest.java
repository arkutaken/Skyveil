package name.skyveil.client.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageViewportTest {
    @Test void hiddenPartOfASlotCannotBeHoveredThroughInventory(){
        assertTrue(StoragePreviewManager.withinViewport(100,199,900,200));
        assertFalse(StoragePreviewManager.withinViewport(100,200,900,200));
        assertFalse(StoragePreviewManager.withinViewport(100,210,900,200));
    }
    @Test void excludesClippedTopAndScrollbarEdge(){
        assertFalse(StoragePreviewManager.withinViewport(100,-1,900,200));
        assertFalse(StoragePreviewManager.withinViewport(890,50,900,200));
        assertTrue(StoragePreviewManager.withinViewport(889,0,900,200));
    }
}