package name.skyveil.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillXpMessageTest {
    @Test void recognizesDefaultSkillProgressIncludingFormattedAndAbbreviatedValues(){
        assertTrue(HudVisibility.isSkillXp("+12.5 Farming (12,000/50,000)"));
        assertTrue(HudVisibility.isSkillXp("\u00a7b+1,234.5\u00a0Combat \u00a77(45%)"));
        assertTrue(HudVisibility.isSkillXp("+1.2k Hunting (50%)"));
    }
    @Test void keepsOtherActionBarMessagesOutOfTheSkillXpRule(){
        assertFalse(HudVisibility.isSkillXp(null));
        assertFalse(HudVisibility.isSkillXp("Not enough Mana!"));
        assertFalse(HudVisibility.isSkillXp("-50 Mana (Teleport)"));
        assertFalse(HudVisibility.isSkillXp("+100 Health"));
        assertFalse(HudVisibility.isSkillXp("Combat Level Up!"));
    }
}