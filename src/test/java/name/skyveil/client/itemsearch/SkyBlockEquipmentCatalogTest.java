package name.skyveil.client.itemsearch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyBlockEquipmentCatalogTest {
    @Test void resolvesEquipmentTypesFromBundledInternalIds(){
        assertEquals(0,SkyBlockEquipmentCatalog.typeForInternalId("MOLTEN_NECKLACE"));
        assertEquals(1,SkyBlockEquipmentCatalog.typeForInternalId("SHADOW_ASSASSIN_CLOAK"));
        assertEquals(2,SkyBlockEquipmentCatalog.typeForInternalId("IMPLOSION_BELT"));
        assertEquals(3,SkyBlockEquipmentCatalog.typeForInternalId("SOULWEAVER_GLOVES"));
    }

    @Test void recognizesEquipmentWordsUsedByDisplayNames(){
        assertEquals(1,SkyBlockEquipmentCatalog.typeFromText("Zorro's Cape"));
        assertEquals(3,SkyBlockEquipmentCatalog.typeFromText("Gauntlet of Contagion"));
    }
}
