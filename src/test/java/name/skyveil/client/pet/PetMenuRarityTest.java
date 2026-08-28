package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetMenuRarityTest {
    @Test void readsPetTierFromHypixelStructuredMetadata(){
        assertEquals(SkyblockRarity.LEGENDARY,PetTracker.metadataRarity("{petInfo:{\\\"type\\\":\\\"GRIFFIN\\\",\\\"tier\\\":\\\"LEGENDARY\\\"}}"));
        assertEquals(SkyblockRarity.MYTHIC,PetTracker.metadataRarity("{\"tier\":\"MYTHIC\"}"));
    }

    @Test void rejectsMetadataWithoutAnExplicitTier(){
        assertNull(PetTracker.metadataRarity("{type:\"GRIFFIN\"}"));
    }
}
