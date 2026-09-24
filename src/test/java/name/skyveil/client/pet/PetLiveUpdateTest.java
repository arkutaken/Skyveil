package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PetLiveUpdateTest {
    @Test void parsesAutopetRuleMessagesWithAndWithoutRuleNumbers() {
        assertEquals(new PetTracker.LivePetChange("Elephant",-1,true),PetTracker.parseLivePetChange("Autopet equipped your Elephant!"));
        assertEquals(new PetTracker.LivePetChange("Golden Dragon",100,true),PetTracker.parseLivePetChange("Autopet Rule 2 equipped your [Lvl 100] Golden Dragon! VIEW RULE"));
    }

    @Test void normalizesFormattedPetRuleNotificationsBeforeMatching() {
        assertEquals(new PetTracker.LivePetChange("Elephant",-1,true),
            PetTracker.parseLivePetChange("\u00a76Autopet Rule 2 \u00a7eequipped your \u00a76Elephant! \u00a7aVIEW RULE"));
        assertEquals(new PetTracker.LivePetChange("Golden Dragon",100,true),
            PetTracker.parseLivePetChange("Auto\u200bpet\u00a0Rule #2: equipped your [Lvl 100] Golden Dragon!"));
    }

    @Test void acceptsDirectEquipNotificationsAndRejectsRuleConfigurationMessages() {
        assertEquals(new PetTracker.LivePetChange("Griffin",86,false),
            PetTracker.parseLivePetChange("You equipped your [Lvl 86] Griffin!"));
        assertNull(PetTracker.parseLivePetChange("Autopet Rule 2 was enabled"));
        assertNull(PetTracker.parseLivePetChange(null));
    }
    @Test void parsesManualSummonMessages() {
        assertEquals(new PetTracker.LivePetChange("Griffin",86,false),PetTracker.parseLivePetChange("You summoned your [Lvl 86] Griffin!"));
        assertNull(PetTracker.parseLivePetChange("Your pet gained experience"));
    }

    @Test void reappliesAnUnchangedWidgetAfterAnotherSourceChangesTheHud() {
        assertFalse(PetTracker.shouldApplyWidget("lion|86","lion|86",12,12));
        assertTrue(PetTracker.shouldApplyWidget("lion|86","lion|86",12,13));
        assertTrue(PetTracker.shouldApplyWidget("griffin|100","lion|86",12,12));
    }

    @Test void unchangedPreEquipWidgetCannotUndoALoadoutOrRuleSelection() {
        assertTrue(PetTracker.shouldDeferWidget("Pig",10_000,"Hedgehog",30_000,"hedgehog|100","hedgehog|100"));
        assertFalse(PetTracker.shouldDeferWidget("Pig",10_000,"Pig",10_100,"hedgehog|100","pig|100"));
        assertFalse(PetTracker.shouldDeferWidget("Pig",10_000,"Elephant",30_000,"hedgehog|100","elephant|100"));
    }
    @Test void readsSameLineWidgetValues() {
        assertEquals("[Lvl 86] Lion",PetTracker.widgetValue("Pet: [Lvl 86] Lion"));
        assertEquals("",PetTracker.widgetValue("Pet:"));
    }

    @Test void deferredOldWidgetIsRetriedAfterTheShortPacketOrderingWindow() {
        long selectedAt=10_000;
        assertTrue(PetTracker.shouldDeferWidget("Pig",selectedAt,"Hedgehog",10_500));
        assertFalse(PetTracker.shouldDeferWidget("Pig",selectedAt,"Hedgehog",11_000));
        assertFalse(PetTracker.shouldDeferWidget("Hedgehog",selectedAt,"Hedgehog",10_100));
        assertFalse(PetTracker.shouldDeferWidget("Hedgehog",selectedAt,"Hedgehog ✦",10_100));
    }
}
