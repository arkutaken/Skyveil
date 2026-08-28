package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetDisplayStyleTest {
    @Test void resolvesEveryConfiguredStyleAndFallsBackToPanel(){
        assertEquals(PetDisplayStyle.PANEL,PetDisplayStyle.from(null));
        assertEquals(PetDisplayStyle.PANEL,PetDisplayStyle.from("invalid"));
        assertEquals(PetDisplayStyle.PANEL,PetDisplayStyle.from("cosmic"));
        assertEquals(PetDisplayStyle.PANEL,PetDisplayStyle.from("RUNIC"));
        assertEquals(PetDisplayStyle.MINIMAL,PetDisplayStyle.from("minimal"));
    }

    @Test void stylesHaveDistinctPresentationAndRarityAwareVariants(){
        var panel=PetDisplayStyle.PANEL.palette(0xFFAA00,150);
        var minimal=PetDisplayStyle.MINIMAL.palette(0xFFAA00,150);
        assertTrue(panel.background());assertFalse(minimal.background());
        assertNotEquals(panel.progressColor(),minimal.progressColor());
    }
}
