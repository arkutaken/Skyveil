package name.skyveil.client.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatParserTest {
    private static StatParser.Reading find(java.util.List<StatParser.Reading> values,PlayerStat stat){
        return values.stream().filter(value->value.stat()==stat).findFirst().orElseThrow();
    }
    @Test void readsScreenshotValuesWithModernGlyphsAndOverflow(){
        var values=StatParser.parse("4,703/4,328\uE010   387\uE008   599/599\uE003 780\uE017   123/123\uE028");
        assertEquals(4,values.size());
        var health=find(values,PlayerStat.HEALTH);
        assertEquals(4703,health.value());assertEquals(4328,health.maximum());assertEquals(1,health.fraction());
        assertEquals(387,find(values,PlayerStat.DEFENSE).value());
        assertEquals(780,find(values,PlayerStat.MANA).overflow());
        assertEquals(123,find(values,PlayerStat.VITALITY).maximum());
    }
    @Test void supportsLegacySymbolsAndFormattedComponents(){
        String message="\u00a7c1,200\u00a7f/\u00a7c2,400❤   \u00a7a300❈ Defense   \u00a7b50/100✎ Mana   400✦ Speed";
        var values=StatParser.parse(message);
        assertEquals(4,values.size());
        assertEquals(.5,find(values,PlayerStat.HEALTH).fraction());
        assertEquals(400,find(values,PlayerStat.SPEED).value());
    }
    @Test void removalRangesPreserveUnrelatedStatusAndAbilityMessages(){
        String message="100/200❤   -50 Mana (Teleport)   387❈ Defense   599/599✎ Mana   +12 Combat (23/100)";
        var values=StatParser.parse(message);
        StringBuilder kept=new StringBuilder();
        for(int i=0;i<message.length();i++){
            int offset=i;
            if(values.stream().noneMatch(value->offset>=value.start()&&offset<value.end()))kept.append(message.charAt(i));
        }
        assertTrue(kept.toString().contains("-50 Mana (Teleport)"));
        assertTrue(kept.toString().contains("+12 Combat (23/100)"));
        assertFalse(kept.toString().contains("Defense"));
        assertEquals(3,values.size());
    }
    @Test void missingPartialAndInvalidValuesNeverBecomeFullBars(){
        assertTrue(StatParser.parse(null).isEmpty());
        assertTrue(StatParser.parse("Not enough mana! -50 Mana (Teleport)").isEmpty());
        assertTrue(StatParser.parse("5/0❤").isEmpty());
        var values=StatParser.parse("0/123\uE028");
        assertEquals(0,values.getFirst().fraction());
        assertEquals(PlayerStat.VITALITY,values.getFirst().stat());
    }
}