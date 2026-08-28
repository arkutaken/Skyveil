package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetTabSectionTest {
    @Test
    void petTrainingStartsANewTabSection() {
        assertTrue(PetTracker.isActivePetSectionBoundary("Pet Training:"));
        assertTrue(PetTracker.isActivePetSectionBoundary("§ePet Training: §r"));
        assertFalse(PetTracker.isActivePetSectionBoundary("[Lvl 86] Lion"));
        assertFalse(PetTracker.isActivePetSectionBoundary("35,893.2/666.7k XP (5.4%)"));
    }
}
