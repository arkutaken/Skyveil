package name.skyveil.client.combat;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One bounded rolling damage window per target, independent of rendering and entities. */
final class DamageBatcher {
    static final int MAX_HITS=5;
    static final long DISPLAY_TICKS=30;
    /** Allows one-second weapon swings to share a rolling window without retaining stale combat. */
    static final long AGGREGATION_TICKS=20;
    private final Map<Integer,Batch> targets=new HashMap<>();

    Batch accept(int targetId,BigInteger damage,long tick){
        Batch batch=targets.computeIfAbsent(targetId,Batch::new);
        if(tick-batch.lastHitTick>AGGREGATION_TICKS)batch.reset();
        if(batch.hits.size()==MAX_HITS)batch.sum=batch.sum.subtract(batch.hits.removeFirst());
        batch.hits.addLast(damage);batch.sum=batch.sum.add(damage);batch.lastHitTick=tick;batch.expiresAt=tick+DISPLAY_TICKS;return batch;
    }
    // Secondary damage augments the most recent melee sample, not the hit count.
    // Without a recent primary sample there is no safe batch to attach it to.
    Batch addSecondary(int targetId,BigInteger damage,long tick){
        Batch batch=targets.get(targetId);if(batch==null||batch.hits.isEmpty()||tick-batch.lastHitTick>AGGREGATION_TICKS)return null;
        BigInteger combined=batch.hits.removeLast().add(damage);batch.hits.addLast(combined);batch.sum=batch.sum.add(damage);batch.expiresAt=tick+DISPLAY_TICKS;return batch;
    }
    void tick(long tick){targets.values().removeIf(batch->tick>batch.expiresAt);}
    void removeTarget(int targetId){targets.remove(targetId);}
    void clear(){targets.clear();}
    int batchCount(){return targets.size();}
    List<Display> displays(){return targets.values().stream().map(batch->new Display(batch.targetId,batch.average(),batch.hits.size())).toList();}

    static final class Batch {
        final int targetId;final ArrayDeque<BigInteger> hits=new ArrayDeque<>(MAX_HITS);BigInteger sum=BigInteger.ZERO;long lastHitTick=Long.MIN_VALUE/2,expiresAt;
        Batch(int targetId){this.targetId=targetId;}
        void reset(){hits.clear();sum=BigInteger.ZERO;}
        BigInteger average(){int count=hits.size();return count==0?BigInteger.ZERO:sum.add(BigInteger.valueOf(count/2L)).divide(BigInteger.valueOf(count));}
    }
    record Display(int targetId,BigInteger average,int hits){}
}
