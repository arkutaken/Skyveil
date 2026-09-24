package name.skyveil.client.mining;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CommissionParserTest {
    @Test void readsPercentCountsAndCompletedOnlyInsideWidget(){
        var result=CommissionParser.parse(List.of("Skills:","Mining: 90%","§6§lCommissions:",
            "§fMithril Miner: §a42.5%","Titanium Miner: 3/10","Goblin Slayer: DONE","",
            "Powders:","Mithril: 50%"));
        assertEquals(3,result.size());assertEquals(.425,result.get(0).fraction());
        assertEquals(.3,result.get(1).fraction());assertEquals("Done",result.get(2).progress());
    }
    @Test void newWidgetSnapshotReplacesOldProgressAndAssignments(){
        assertEquals(.1,CommissionParser.parse(List.of("Commissions:","Miner: 10%")).getFirst().fraction());
        assertEquals(.8,CommissionParser.parse(List.of("Commissions:","Miner: 80%")).getFirst().fraction());
        assertEquals("Slayer",CommissionParser.parse(List.of("Commissions:","Slayer: 0%")).getFirst().name());
        assertTrue(CommissionParser.parse(List.of("Skills:","Mining: 90%")).isEmpty());
    }
    @Test void malformedCountersCannotBreakParsingOrLeakNextSection(){
        var rows=CommissionParser.parse(List.of("Commissions (2/4):","Miner: 0/0","Slayer: 1,000/2,000","Pets:","Pet: 50%"));
        assertEquals(1,rows.size());assertEquals(.5,rows.getFirst().fraction());
    }
}
