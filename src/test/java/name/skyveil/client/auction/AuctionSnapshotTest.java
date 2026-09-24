package name.skyveil.client.auction;

import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AuctionSnapshotTest {
    static JsonObject listing(String id,int count,double price,boolean modern) throws Exception {
        var extra=new CompoundTag();extra.putString("id",id);
        var item=new CompoundTag();
        if(modern){
            var components=new CompoundTag();components.put("minecraft:custom_data",extra);
            item.put("components",components);item.putInt("count",count);
        }else{
            var tag=new CompoundTag();tag.put("ExtraAttributes",extra);
            item.put("tag",tag);item.putByte("Count",(byte)count);
        }
        var entries=new ListTag();entries.add(item);
        var root=new CompoundTag();root.put("i",entries);
        var bytes=new ByteArrayOutputStream();NbtIo.writeCompressed(root,bytes);
        var auction=new JsonObject();
        auction.addProperty("bin",true);auction.addProperty("claimed",false);
        auction.addProperty("end",System.currentTimeMillis()+600_000);
        auction.addProperty("starting_bid",price);
        auction.addProperty("item_bytes",Base64.getEncoder().encodeToString(bytes.toByteArray()));
        return auction;
    }
    static JsonObject page(int number,int total,long generation,JsonObject... listings){
        var result=new JsonObject();result.addProperty("page",number);
        result.addProperty("totalPages",total);result.addProperty("lastUpdated",generation);
        var auctions=new com.google.gson.JsonArray();
        for(var listing:listings)auctions.add(listing);
        result.add("auctions",auctions);return result;
    }
    @Test void decodesLegacyByteCountsAndModernComponents() throws Exception {
        assertEquals(new AuctionPrices.Listing("ASPECT_OF_THE_END",100),AuctionPrices.decode(listing("ASPECT_OF_THE_END",2,200,false)));
        var modern=listing("WITHER_BOOTS",1,800000,true);
        var wrapped=new JsonObject();wrapped.add("data",modern.get("item_bytes"));modern.add("item_bytes",wrapped);
        assertEquals(new AuctionPrices.Listing("WITHER_BOOTS",800000),AuctionPrices.decode(modern));
    }
    @Test void mergesEveryPageUsingLowestUnitPrice() throws Exception {
        var first=page(0,2,10,listing("A",10,1000,false),listing("B",1,70,true));
        var second=page(1,2,10,listing("A",2,100,true),listing("C",1,30,false));
        var result=AuctionPrices.download(p->p==0?first:second);
        assertEquals(java.util.Map.of("A",50.0,"B",70.0,"C",30.0),result.prices());
    }
    @Test void rolloverRetriesWithoutLeakingOldGenerationPrices() throws Exception {
        var old=page(0,2,10,listing("OLD",1,1,false));
        var fresh=page(0,2,11,listing("NEW",1,20,false));
        var last=page(1,2,11,listing("NEW",1,10,false));
        var attempts=new AtomicInteger();
        var result=AuctionPrices.download(p->p==0?(attempts.incrementAndGet()==1?old:fresh):last);
        assertEquals(2,attempts.get());
        assertEquals(java.util.Map.of("NEW",10.0),result.prices());
    }
    @Test void persistentRolloverStopsAfterThreeAttempts() throws Exception {
        var first=page(0,2,1,listing("A",1,10,false));
        var second=page(1,2,2,listing("B",1,20,false));
        var attempts=new AtomicInteger();
        assertThrows(Exception.class,()->AuctionPrices.download(p->{if(p==0)attempts.incrementAndGet();return p==0?first:second;}));
        assertEquals(3,attempts.get());
    }
    @Test void httpFailureDoesNotTriggerAnImmediateRetryLoop(){
        var attempts=new AtomicInteger();
        assertThrows(Exception.class,()->AuctionPrices.download(p->{attempts.incrementAndGet();throw new java.io.IOException("HTTP 429");}));
        assertEquals(1,attempts.get());
    }
    @Test void malformedListingDoesNotDiscardOtherItems() throws Exception {
        var bad=listing("BAD",1,10,false);bad.addProperty("item_bytes","invalid");
        var first=page(0,1,1,bad,listing("A",1,10,false),listing("B",1,20,true));
        assertEquals(2,AuctionPrices.download(p->first).prices().size());
    }
    @Test void almostEntirelyMalformedSnapshotIsRejected() throws Exception {
        var bad=listing("BAD",1,10,false);bad.addProperty("item_bytes","invalid");
        var first=page(0,1,1,bad,bad,bad,bad,listing("A",1,10,false));
        assertThrows(Exception.class,()->AuctionPrices.download(p->first));
    }
    @Test void invalidStackAmountsCannotProducePrices() throws Exception {
        assertThrows(Exception.class,()->AuctionPrices.decode(listing("A",0,20,true)));
        assertThrows(Exception.class,()->AuctionPrices.decode(listing("A",1,-20,true)));
    }
    @Test void onlyActualSoulboundMarkersExcludeItems(){
        assertTrue(AuctionTooltip.soulbound(List.of(Component.literal("\u00a78\u00a7l* Co-op Soulbound *"))));
        assertTrue(AuctionTooltip.soulbound(List.of(Component.literal("Soulbound"))));
        assertFalse(AuctionTooltip.soulbound(List.of(Component.literal("Can be made soulbound at the Museum."))));
    }
    @Test void combinedBooksMatchByAllEnchantmentsRegardlessOfOrder(){
        var extra=new CompoundTag();extra.putString("id","ENCHANTED_BOOK");
        var enchants=new CompoundTag();enchants.putInt("sharpness",6);enchants.putInt("critical",5);
        extra.put("enchantments",enchants);
        assertEquals("BOOK:CRITICAL=5,SHARPNESS=6",AuctionPrices.identity(extra));
        enchants.putInt("critical",6);
        assertEquals("BOOK:CRITICAL=6,SHARPNESS=6",AuctionPrices.identity(extra));
    }
    @Test void distinguishesRuneTypesAndLevelsAndEscapedPetJson(){
        var extra=new CompoundTag();extra.putString("id","RUNE");
        var runes=new CompoundTag();runes.putInt("SNOW",3);extra.put("runes",runes);
        assertEquals("RUNE:SNOW:3",AuctionPrices.identity(extra));
        extra.putString("id","PET");
        extra.putString("petInfo","{\\\"type\\\":\\\"HEDGEHOG\\\",\\\"tier\\\":\\\"LEGENDARY\\\"}");
        assertEquals("PET:HEDGEHOG:LEGENDARY:LVL:1",AuctionPrices.identity(extra));
    }

    @Test void boundedPrefetchStillProcessesEveryPageExactlyOnce() throws Exception {
        var visits=new java.util.concurrent.atomic.AtomicIntegerArray(9);
        var result=AuctionPrices.download(p->{
            visits.incrementAndGet(p);
            return page(p,9,25,listing("ITEM_"+p,1,100+p,false));
        });
        assertEquals(9,result.prices().size());
        for(int i=0;i<9;i++){
            assertEquals(1,visits.get(i));
            assertEquals(100.0+i,result.prices().get("ITEM_"+i));
        }
    }
}