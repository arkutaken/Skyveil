package name.skyveil.client.auction;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuctionHistoryTest {
    @Test void sameGenerationIsNotStoredAgainAndSameHourReplacesPreviousObservation(){
        var history=new AuctionHistory();long now=100*AuctionHistory.HOUR;
        assertTrue(history.update(1,Map.of("A",10.0,"OLD",5.0),now));
        assertFalse(history.update(1,Map.of("A",999.0),now+1000));
        assertTrue(history.update(2,Map.of("A",20.0),now+2000));
        assertEquals(20.0,history.average("A",now+2000).price());
        assertEquals(1,history.average("A",now+2000).observations());
        assertNull(history.average("OLD",now+2000));
        assertEquals(1,history.save().getCompoundOrEmpty("items").size());
    }
    @Test void averagesObservedHoursAndPrunesExpiredValuesIncludingAfterReload(){
        var history=new AuctionHistory();long now=100*AuctionHistory.HOUR;
        history.update(1,Map.of("A",10.0),now);
        history.update(2,Map.of("A",30.0),now+AuctionHistory.HOUR);
        assertEquals(20.0,history.average("A",now+AuctionHistory.HOUR).price());
        assertFalse(history.average("A",now+AuctionHistory.HOUR).complete());
        var restored=new AuctionHistory();restored.load(history.saveForCache(),now+AuctionHistory.WINDOW);
        assertEquals(30.0,restored.average("A",now+AuctionHistory.WINDOW).price());
        assertTrue(restored.average("A",now+AuctionHistory.WINDOW).complete());
        restored.update(3,Map.of("B",50.0),now+AuctionHistory.WINDOW+AuctionHistory.HOUR);
        assertNull(restored.average("A",now+AuctionHistory.WINDOW+AuctionHistory.HOUR));
        assertEquals(1,restored.save().getCompoundOrEmpty("items").size());
    }
}
