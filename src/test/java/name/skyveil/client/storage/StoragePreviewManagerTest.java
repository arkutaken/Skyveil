package name.skyveil.client.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoragePreviewManagerTest {
    @Test void recognizesOpenedStoragePages(){
        assertEquals("ender:1",StoragePreviewManager.storageKeyFromTitle("Ender Chest (1/9)"));
        assertEquals("ender:7",StoragePreviewManager.storageKeyFromTitle("Ender Chest Page (7/9)"));
        assertEquals("backpack:1",StoragePreviewManager.storageKeyFromTitle("Large Backpack (Slot #1)"));
        assertNull(StoragePreviewManager.storageKeyFromTitle("Storage"));
    }

    @Test void mapsStorageOverviewSlotsToTheSamePersistentKeys(){
        assertTrue(StoragePreviewManager.isStorageOverview("Storage"));
        assertEquals("ender:1",StoragePreviewManager.storageKeyForOverviewSlot(9));
        assertEquals("ender:9",StoragePreviewManager.storageKeyForOverviewSlot(17));
        assertEquals("backpack:1",StoragePreviewManager.storageKeyForOverviewSlot(27));
        assertEquals("backpack:18",StoragePreviewManager.storageKeyForOverviewSlot(44));
        assertNull(StoragePreviewManager.storageKeyForOverviewSlot(18));
        assertEquals(9,StoragePreviewManager.slotForStorageKey("ender:1"));
        assertEquals(44,StoragePreviewManager.slotForStorageKey("backpack:18"));
        assertEquals(-1,StoragePreviewManager.slotForStorageKey("invalid"));
    }
}
