package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetTrainingDetectionTest {
    @Test
    void recognizesOnlyTheTrainingOverviewTitle() {
        assertTrue(PetTracker.isPetTrainingOverview("Pet Training"));
        assertTrue(PetTracker.isPetTrainingOverview("§aPet Training (1/3)"));
        assertFalse(PetTracker.isPetTrainingOverview("Training Slot 1"));
        assertFalse(PetTracker.isPetTrainingOverview("(1/3) Pets"));
    }

    @Test
    void recognizesTrainingStatusWithoutMatchingNormalPetAbilities() {
        assertTrue(PetTracker.isTrainingPetLore(List.of("§eThis pet is currently training!")));
        assertTrue(PetTracker.isTrainingPetLore(List.of("§cClick to cancel training!")));
        assertFalse(PetTracker.isTrainingPetLore(List.of("Efficient Trainer", "Makes training sessions more efficient.")));
        assertFalse(PetTracker.isTrainingPetLore(List.of("§eClick to summon!")));
    }
}
