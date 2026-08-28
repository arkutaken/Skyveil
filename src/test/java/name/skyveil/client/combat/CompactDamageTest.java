package name.skyveil.client.combat;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompactDamageTest {
    @Test void settingDefaultsToMeleeOnlyAndUsesMinimalStyle(){var config=new name.skyveil.client.config.SkyveilConfig();assertFalse(config.compactDamage);assertEquals("MINIMAL",config.compactDamageStyle);assertFalse(config.compactDamageCrimsonSwipe);assertFalse(config.compactDamageFerocity);assertFalse(config.compactDamageVenomous);assertFalse(config.compactDamageFire);assertFalse(config.compactDamageThunderlord);assertFalse(config.compactDamagePet);assertFalse(config.compactDamageOther);}
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
    @Test void secondaryDamageAddsToLatestMeleeWithoutConsumingAHit(){DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch batch=batches.accept(1,BigInteger.valueOf(100),1);batches.accept(1,BigInteger.valueOf(200),2);assertSame(batch,batches.addSecondary(1,BigInteger.valueOf(50),3));assertEquals(2,batch.hits.size());assertEquals(BigInteger.valueOf(175),batch.average());}
    @Test void orphanedSecondaryDamageCannotCreateACompactWindow(){DamageBatcher batches=new DamageBatcher();assertNull(batches.addSecondary(1,BigInteger.TEN,1));assertEquals(0,batches.batchCount());}
    @Test void slowerAttacksStillBuildTheFiveHitAverage(){
        DamageBatcher batches=new DamageBatcher();DamageBatcher.Batch batch=null;
        for(int hit=1;hit<=5;hit++)batch=batches.accept(7,BigInteger.valueOf(hit*100L),hit*15L);
        assertNotNull(batch);assertEquals(5,batch.hits.size());assertEquals(BigInteger.valueOf(300),batch.average());
    }
    @Test void displayStylesExposeDamageAndOptionalWindowProgress(){
        DamageBatcher.Display display=new DamageBatcher.Display(1,BigInteger.valueOf(12_345),4);
        assertEquals("12,345",CompactDamageRenderer.label(display,"MINIMAL").getString());
        assertEquals("✦ 12,345 ✦ [4/5]",CompactDamageRenderer.label(display,"NEON").getString());
        assertEquals("⚔ 12,345 [4/5]",CompactDamageRenderer.label(display,"CRIMSON").getString());
        assertEquals("12,345",CompactDamageRenderer.label(display,"UNKNOWN").getString());
    }
    @Test void styledSecondaryDamageSourcesStaySeparate(){
        assertEquals(DamageSource.VENOMOUS,DamageSourceClassifier.styledSource(net.minecraft.network.chat.Component.literal("1200").withStyle(net.minecraft.ChatFormatting.GREEN)));
        assertEquals(DamageSource.FIRE,DamageSourceClassifier.styledSource(net.minecraft.network.chat.Component.literal("1200").withStyle(net.minecraft.ChatFormatting.GOLD)));
        assertEquals(DamageSource.THUNDERLORD,DamageSourceClassifier.styledSource(net.minecraft.network.chat.Component.literal("1200").withStyle(net.minecraft.ChatFormatting.BLUE)));
        assertEquals(DamageSource.PET,DamageSourceClassifier.styledSource(net.minecraft.network.chat.Component.literal("1200").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)));
        var critical=net.minecraft.network.chat.Component.literal("12").withStyle(net.minecraft.ChatFormatting.WHITE).append(net.minecraft.network.chat.Component.literal("00").withStyle(net.minecraft.ChatFormatting.RED));
        assertEquals(DamageSource.MELEE,DamageSourceClassifier.styledSource(critical));
    }
    @Test void physicalFollowUpsDistinguishCrimsonFerocityAndOther(){
        BigInteger primary=BigInteger.valueOf(100_000);
        assertEquals(DamageSource.CRIMSON_SWIPE,DamageSourceClassifier.physicalFollowUp(primary,BigInteger.valueOf(25_000),true));
        assertEquals(DamageSource.FEROCITY,DamageSourceClassifier.physicalFollowUp(primary,BigInteger.valueOf(95_000),true));
        assertEquals(DamageSource.OTHER,DamageSourceClassifier.physicalFollowUp(primary,BigInteger.valueOf(60_000),false));
    }
}
