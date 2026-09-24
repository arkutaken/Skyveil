package name.skyveil.client.auction;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static name.skyveil.client.auction.AuctionSnapshotTest.*;

class AuctionAveragesTest {
    @Test void averagesFiveCheapestUnitPricesAcrossPagesAndExcludesPurchasedBins() throws Exception{
        var sold=listing("A",1,1,false);sold.addProperty("highest_bid_amount",1);
        var first=page(0,2,10,listing("A",2,20,false),listing("A",1,20,false),listing("A",1,30,false),sold);
        var second=page(1,2,10,listing("A",1,40,false),listing("A",1,50,false),listing("A",1,999999,false));
        var result=AuctionPrices.download(p->p==0?first:second);
        assertEquals(10.0,result.prices().get("A"));
        assertEquals(30.0,result.averages().get("A"));
        assertEquals(30.0,result.unmodified().get("A"));
    }
    @Test void usesAvailableSparseListingsWithoutInventingSamples() throws Exception{
        var first=page(0,1,10,listing("A",1,10,false),listing("A",1,30,false));
        assertEquals(20.0,AuctionPrices.download(p->first).averages().get("A"));
    }
    @Test void purchasedBinWithBidsAndDuplicateUuidDoNotContribute() throws Exception{
        var sold=listing("A",1,1,false);
        sold.add("bids",com.google.gson.JsonParser.parseString("[{}]"));
        var a=listing("A",1,10,false);a.addProperty("uuid","same");
        var b=listing("A",1,30,false);b.addProperty("uuid","other");
        assertEquals(20.0,AuctionPrices.download(p->page(0,1,10,a,a,b,sold)).averages().get("A"));
    }
    @Test void unchangedGenerationDoesNotDownloadRemainingPagesOrReplaceCache() throws Exception{
        var previous=AuctionPrices.download(p->page(0,1,10,listing("A",1,10,false)));
        var calls=new AtomicInteger();
        var result=AuctionPrices.updatedSnapshot(p->{calls.incrementAndGet();return page(0,100,10);},previous);
        assertSame(previous,result);assertEquals(1,calls.get());
        var newer=AuctionPrices.updatedSnapshot(p->page(0,1,11,listing("B",1,20,false)),previous);
        assertEquals(Map.of("B",20.0),newer.prices());
        assertFalse(newer.averages().containsKey("A"));
    }
}
