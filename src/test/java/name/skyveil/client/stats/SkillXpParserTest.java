package name.skyveil.client.stats;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillXpParserTest {
    @Test void extractsScreenshotProgressAndKeepsSurroundingStats(){
        String text="4,703/4,328\uE010   +24 Farming (158,094/217,000)   484/484\uE003";
        var value=SkillXpParser.parse(text);
        assertNotNull(value);assertEquals("Farming",value.skill());assertEquals("+24",value.gain());
        assertEquals(158094d/217000,value.fraction(),.00001);
        String remaining=text.substring(0,value.start())+text.substring(value.end());
        assertTrue(remaining.contains("4,703/4,328"));assertTrue(remaining.contains("484/484"));
        assertFalse(remaining.contains("Farming"));
    }
    @Test void supportsFormattingPercentagesAndAbbreviations(){
        var formatted=SkillXpParser.parse("\u00a7b+1,234.5\u00a0Combat \u00a77(45%)");
        assertEquals(.45,formatted.fraction(),.00001);
        assertEquals(.5,SkillXpParser.parse("+1.2k Mining (100k/200k)").fraction(),.00001);
        assertEquals(-1,SkillXpParser.parse("+24 Farming").fraction());
    }
    @Test void leavesAbilityAndUnrelatedMessagesAlone(){
        assertNull(SkillXpParser.parse("-50 Mana (Teleport)"));
        assertNull(SkillXpParser.parse("Not enough Mana!"));
        assertNull(SkillXpParser.parse(null));
        assertEquals(-1,SkillXpParser.parse("+12 Farming (50/0)").fraction());
    }
}