package name.skyveil.client.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoPetRuleMessageFilterTest {
    @Test void acceptsHypixelAutopetLayoutVariants(){
        assertTrue(AutoPetRuleMessageFilter.matchesText("Autopet equipped your [Lvl 100] Golden Dragon! VIEW RULE"));
        assertTrue(AutoPetRuleMessageFilter.matchesText("§6Autopet Rule 2 §eequipped your Elephant! §aVIEW RULE"));
        assertTrue(AutoPetRuleMessageFilter.matchesText("Auto Pet equipped your Enderman!"));
    }

    @Test void remainsSpecificAndHonorsToggleAndOverlay(){
        assertFalse(AutoPetRuleMessageFilter.matchesText("You equipped your Golden Dragon!"));
        assertFalse(AutoPetRuleMessageFilter.matchesText("Autopet rule was enabled"));
        assertTrue(AutoPetRuleMessageFilter.shouldSuppressText("Autopet equipped your Sheep!",false,true));
        assertFalse(AutoPetRuleMessageFilter.shouldSuppressText("Autopet equipped your Sheep!",true,true));
        assertFalse(AutoPetRuleMessageFilter.shouldSuppressText("Autopet equipped your Sheep!",false,false));
    }
}
