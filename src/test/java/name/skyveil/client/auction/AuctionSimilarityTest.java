package name.skyveil.client.auction;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static name.skyveil.client.auction.AuctionSnapshotTest.*;

class AuctionSimilarityTest {
    private static CompoundTag drill(){
        var extra=new CompoundTag();extra.putString("id","TITANIUM_DRILL_4");
        extra.putString("modifier","auspicious");extra.putInt("rarity_upgrades",1);
        extra.putString("drill_part_engine","ruby_polished_drill_engine");
        extra.putString("drill_part_fuel_tank","gemstone_fuel_tank");
        extra.putString("drill_part_upgrade_module","goblin_omelette_spicy");
        var enchants=new CompoundTag();enchants.putInt("compact",6);enchants.putInt("ultimate_wise",5);
        extra.put("enchantments",enchants);
        var gems=new CompoundTag();gems.putString("JADE_0","PERFECT");extra.put("gems",gems);
        return extra;
    }
    static com.google.gson.JsonObject auction(CompoundTag extra,double price) throws Exception{
        var result=listing(extra.getStringOr("id",""),1,price,false);
        var tag=new CompoundTag();tag.put("ExtraAttributes",extra);
        var item=new CompoundTag();item.put("tag",tag);item.putByte("Count",(byte)1);
        var entries=new ListTag();entries.add(item);var root=new CompoundTag();root.put("i",entries);
        var bytes=new ByteArrayOutputStream();NbtIo.writeCompressed(root,bytes);
        result.addProperty("item_bytes",Base64.getEncoder().encodeToString(bytes.toByteArray()));return result;
    }
    @Test void everyRelevantUpgradeSeparatesComparables(){
        var base=drill();String key=AuctionSimilarity.key(base);
        for(String field:List.of("modifier","drill_part_engine","drill_part_fuel_tank","drill_part_upgrade_module",
            "enchantments","gems","rarity_upgrades")){
            var changed=base.copy();changed.remove(field);assertNotEquals(key,AuctionSimilarity.key(changed),field);
        }
        var changed=base.copy();changed.putInt("upgrade_level",5);assertNotEquals(key,AuctionSimilarity.key(changed));
        changed=base.copy();changed.getCompoundOrEmpty("enchantments").putInt("compact",7);
        assertNotEquals(key,AuctionSimilarity.key(changed));
    }
    @Test void ownershipFuelCountersCaseAndCompoundOrderDoNotAffectMatching(){
        var a=drill();var b=drill();
        b.putString("uuid","different");b.putInt("drill_fuel",123);b.putLong("compact_blocks",61000);
        b.putBoolean("donated_museum",true);b.putInt("soulbound",1);b.putString("drill_part_engine","RUBY_POLISHED_DRILL_ENGINE");
        var enchants=new CompoundTag();enchants.putInt("ultimate_wise",5);enchants.putInt("compact",6);b.put("enchantments",enchants);
        assertEquals(AuctionSimilarity.key(a),AuctionSimilarity.key(b));
    }
    @Test void averageUsesOnlyMatchingUpgradedListingsAndRequiresAnActiveMatch() throws Exception{
        var upgraded=drill();var plain=new CompoundTag();plain.putString("id","TITANIUM_DRILL_4");
        var snapshot=AuctionPrices.download(p->page(0,1,10,auction(plain,10),
            auction(upgraded,100),auction(upgraded,200),auction(upgraded,300)));
        assertEquals(10.0,snapshot.prices().get("TITANIUM_DRILL_4"));
        long now=snapshot.received();var history=new AuctionHistory();
        history.update(10,snapshot.similar(),now);
        assertEquals(200.0,AuctionPrices.similarAverage(snapshot,history,upgraded,now).price());
        var absent=upgraded.copy();absent.putInt("hot_potato_count",10);
        assertNull(AuctionPrices.similarAverage(snapshot,history,absent,now));
        var next=AuctionPrices.download(p->page(0,1,11,auction(plain,10)));
        assertNull(AuctionPrices.similarAverage(next,history,upgraded,next.received()));
        assertNull(AuctionPrices.similarAverage(snapshot,history,upgraded,now+900001));
    }
    @Test void oldBaseHistoryCannotContaminateUpgradeHistory(){
        var old=new CompoundTag();old.putInt("schema",1);old.putLong("generation",500);
        var history=new AuctionHistory();history.load(old,1000);
        assertTrue(history.update(1,Map.of("upgrade-key",200.0),1000));
        assertEquals(200.0,history.average("upgrade-key",1000).price());
    }
    @Test void noMatchMessageDoesNotShowABasePriceOrCollectingLabel(){
        assertEquals("3-day average: No similar items found, cannot calculate",AuctionTooltip.historyLine(null,1).getString());
    }
}
