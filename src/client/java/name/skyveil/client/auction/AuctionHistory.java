package name.skyveil.client.auction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import java.io.*;
import java.util.*;

/**
 * One latest observed active-listing average per hour, retained for at most 72 hours.
 * This mutable object is not internally synchronized: AuctionPrices holds its
 * monitor for live reads, updates, loading, and serialization.
 */
public final class AuctionHistory {
    static final long HOUR=3_600_000L,WINDOW=72*HOUR;
    private final NavigableMap<Long,Observation> hours=new TreeMap<>();
    private long generation;
    public record Average(double price,int observations,boolean complete){}
    private record Observation(long time,Map<String,Double> prices){}

    boolean update(long updated,Map<String,Double> prices,long now){
        prune(now);
        if(updated<=generation)return false;
        generation=updated;
        // Replace this hour's sample instead of weighting frequently refreshed
        // hours more heavily than sparsely observed ones.
        hours.put(now/HOUR,new Observation(now,Map.copyOf(prices)));
        return true;
    }
    /**
     * Weights each observed hour equally; missing hours are not zero-price samples.
     * The complete flag describes the age spanned, not uninterrupted coverage.
     */
    Average average(String key,long now){
        double total=0;int count=0;long oldest=now;
        for(var observation:hours.values()){
            if(observation.time()<=now-WINDOW||observation.time()>now)continue;
            Double price=observation.prices().get(key);
            if(price!=null){total+=price;count++;oldest=Math.min(oldest,observation.time());}
        }
        return count==0?null:new Average(total/count,count,now-oldest>=WINDOW-HOUR);
    }
    private void prune(long now){hours.values().removeIf(value->value.time()<=now-WINDOW||value.time()>now);}
    CompoundTag save(){
        var root=new CompoundTag();root.putInt("schema",2);root.putLong("generation",generation);
        var series=new TreeMap<String,List<Long>>();
        for(var observation:hours.values())observation.prices().forEach((key,price)->{
            var values=series.computeIfAbsent(key,ignored->new ArrayList<>());
            values.add(observation.time());values.add(Double.doubleToLongBits(price));
        });
        var items=new CompoundTag();
        series.forEach((key,values)->items.putLongArray(key,values.stream().mapToLong(Long::longValue).toArray()));
        root.put("items",items);return root;
    }
    /** Compress variant history inside the shared cache so upgrade combinations fit its section budget. */
    CompoundTag saveForCache(){
        try{
            var bytes=new ByteArrayOutputStream();NbtIo.writeCompressed(save(),bytes);
            var result=new CompoundTag();result.putInt("schema",2);
            result.putByteArray("compressed",bytes.toByteArray());return result;
        }catch(IOException error){throw new IllegalStateException("Could not encode auction history",error);}
    }
    void load(CompoundTag root,long now){
        if(root!=null&&root.contains("compressed")){
            try{
                byte[] bytes=root.getByteArray("compressed").orElse(new byte[0]);
                if(bytes.length>8*1024*1024)return;
                root=NbtIo.readCompressed(new ByteArrayInputStream(bytes),NbtAccounter.create(128L*1024*1024));
            }catch(IOException|RuntimeException invalid){return;}
        }
        if(root==null||root.getIntOr("schema",0)!=2)return;
        var items=root.getCompoundOrEmpty("items");
        var restored=new TreeMap<Long,Map<String,Double>>();int count=0;
        for(String key:items.keySet()){
            if(++count>100000)break;
            if(key.length()>512)continue;
            long[] values=items.getLongArray(key).orElse(new long[0]);
            if(values.length>146||values.length%2!=0)continue;
            for(int i=0;i<values.length;i+=2){
                long time=values[i];double price=Double.longBitsToDouble(values[i+1]);
                if(time<=now-WINDOW||time>now||!Double.isFinite(price)||price<=0)continue;
                restored.computeIfAbsent(time,ignored->new HashMap<>()).put(key,price);
            }
        }
        restored.forEach((time,prices)->{
            var existing=hours.get(time/HOUR);
            if(existing==null||existing.time()<time)hours.put(time/HOUR,new Observation(time,Map.copyOf(prices)));
        });
        // The latest live snapshot may have arrived while the persistent cache was loading.
        generation=Math.max(generation,root.getLongOr("generation",0));
        prune(now);
    }
}
