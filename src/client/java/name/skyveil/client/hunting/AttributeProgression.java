package name.skyveil.client.hunting;

import name.skyveil.client.itemrarity.SkyblockRarity;

/**
 * Post-Foraging attributes are levelled by syphoning base shards, not by the old
 * equal-level fusion formula. Each array is the server-documented incremental cost
 * for tiers I through X; rarity therefore determines the cumulative maximum.
 */
public final class AttributeProgression {
    public static final int MAX_TIER=10;
    private static final int[] COMMON={1,3,5,6,7,8,10,14,18,24};
    private static final int[] UNCOMMON={1,2,3,4,5,6,7,8,12,16};
    private static final int[] RARE={1,2,3,3,4,4,5,6,8,12};
    private static final int[] EPIC={1,1,2,2,3,3,4,4,5,7};
    private static final int[] LEGENDARY={1,1,1,2,2,2,3,3,4,5};
    private AttributeProgression(){}

    public static Result calculate(SkyblockRarity rarity,int currentTier,int shardsToNext,long ownedBaseShards){
        int[] costs=costs(rarity);
        if(costs==null||currentTier<0||currentTier>MAX_TIER||currentTier<MAX_TIER&&shardsToNext<0)return Result.unknown();
        long maximum=sum(costs,MAX_TIER),syphoned;
        if(currentTier==MAX_TIER)syphoned=maximum;
        else{
            long nextThreshold=sum(costs,currentTier+1);
            if(shardsToNext>costs[currentTier])return Result.unknown();
            syphoned=nextThreshold-shardsToNext;
        }
        long rawRemaining=Math.max(0,maximum-syphoned);
        long purchaseRemaining=Math.max(0,rawRemaining-Math.max(0,ownedBaseShards));
        return new Result(true,currentTier,syphoned,maximum,rawRemaining,purchaseRemaining);
    }

    public static long maximum(SkyblockRarity rarity){int[] costs=costs(rarity);return costs==null?-1:sum(costs,MAX_TIER);}
    private static long sum(int[] values,int count){long total=0;for(int index=0;index<count;index++)total+=values[index];return total;}
    private static int[] costs(SkyblockRarity rarity){
        if(rarity==null)return null;
        return switch(rarity){case COMMON->COMMON;case UNCOMMON->UNCOMMON;case RARE->RARE;case EPIC->EPIC;case LEGENDARY->LEGENDARY;default->null;};
    }
    public record Result(boolean known,int tier,long syphoned,long maximum,long rawRemaining,long purchaseRemaining){
        static Result unknown(){return new Result(false,-1,0,0,0,0);}
        public boolean maxed(){return known&&tier==MAX_TIER;}
    }
}
