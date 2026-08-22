package name.skyveil.client.combat;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompactDamageTest {
    @Test void settingDefaultsOff(){assertFalse(new name.skyveil.client.config.SkyveilConfig().compactDamage);}
    @Test void parsesVerifiedDamageOnlyForms(){
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("12500").orElseThrow().value());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("12,500").orElseThrow().value());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("12.5k").orElseThrow().value());
        assertEquals(new BigInteger("2300000"),DamageTextParser.parse("2.3M").orElseThrow().value());
        assertTrue(DamageTextParser.parse("✧12,500✧").orElseThrow().decorated());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("✯12,500✯⚔").orElseThrow().value());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("12,500♞").orElseThrow().value());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("✷12,500").orElseThrow().value());
        assertEquals(new BigInteger("12500"),DamageTextParser.parse("12,500❁").orElseThrow().value());
    }
    @Test void rejectsHealthMobAndAmbiguousLabels(){
        assertTrue(DamageTextParser.parse("Zombie 12,500/20,000❤").isEmpty());
        assertTrue(DamageTextParser.parse("12,500 HP").isEmpty());
        assertTrue(DamageTextParser.parse("Bob").isEmpty());
        assertTrue(DamageTextParser.parse("12.5").isEmpty());
        assertTrue(DamageTextParser.parse("0").isEmpty());
        assertTrue(DamageTextParser.parse("-12,500").isEmpty());
        assertTrue(DamageTextParser.parse("(12,500)").isEmpty());
    }
    @Test void fiveHitsAverageAndSixthRollsTheSingleWindow(){
        DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch window=null;
        for(long value:new long[]{12_500,13_200,11_900,14_100,12_800})window=batches.accept(90,BigInteger.valueOf(value),1);
        assertNotNull(window);assertEquals(5,window.hits.size());assertEquals(new BigInteger("12900"),window.average());assertEquals(1,batches.batchCount());
        DamageBatcher.Batch same=batches.accept(90,BigInteger.valueOf(15_000),2);assertSame(window,same);assertEquals(5,same.hits.size());assertEquals(new BigInteger("13400"),same.average());assertEquals(1,batches.displays().size());
    }
    @Test void partialWindowRemainsVisibleAndLateHitStartsFreshAverage(){
        DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch batch=batches.accept(4,BigInteger.TEN,5);batches.tick(5+DamageBatcher.AGGREGATION_TICKS+1);assertEquals(BigInteger.TEN,batches.displays().getFirst().average());
        batches.accept(4,BigInteger.valueOf(30),5+DamageBatcher.AGGREGATION_TICKS+2);assertEquals(1,batch.hits.size());assertEquals(BigInteger.valueOf(30),batch.average());
    }
    @Test void separateTargetsNeverMerge(){
        DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch first=batches.accept(10,BigInteger.ONE,1);
        assertNotSame(first,batches.accept(11,BigInteger.TWO,2));assertEquals(2,batches.batchCount());
    }
    @Test void expiryRemovalDisableAndWorldResetClearState(){
        DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch batch=batches.accept(10,BigInteger.ONE,1);batches.tick(batch.expiresAt+1);assertTrue(batches.displays().isEmpty());
        batches.accept(10,BigInteger.ONE,1);batches.removeTarget(10);assertEquals(0,batches.batchCount());batches.accept(11,BigInteger.ONE,1);batches.clear();assertEquals(0,batches.batchCount());
    }
    @Test void arbitraryPrecisionTotalsCannotWrap(){
        DamageBatcher batches=new DamageBatcher();BigInteger huge=new BigInteger("999999999999999999999999999999999999");DamageBatcher.Batch batch=batches.accept(1,huge,1);batches.accept(1,huge,2);assertEquals(huge,batch.average());
        assertEquals("999,999,999,999,999,999,999,999,999,999,999,999",DamageTextParser.format(batch.average()));
    }
    @Test void averageRoundsToNearestWholeDamage(){DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch batch=batches.accept(1,BigInteger.ONE,1);batches.accept(1,BigInteger.TWO,2);assertEquals(BigInteger.TWO,batch.average());}
}
