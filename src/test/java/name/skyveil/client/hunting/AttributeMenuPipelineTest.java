package name.skyveil.client.hunting;

import com.google.gson.JsonParser;
import name.skyveil.client.itemrarity.SkyblockRarity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttributeMenuPipelineTest {
    @Test void rarityCatalogContainsEveryConsumableAttribute(){
        assertEquals(320,AttributeShardResolver.catalogSize());
        assertEquals(SkyblockRarity.COMMON,AttributeShardResolver.rarity("NATURE_ELEMENTAL"));
        assertEquals(SkyblockRarity.LEGENDARY,AttributeShardResolver.rarity("MIRACLE_CHANCE"));
        assertNull(AttributeShardResolver.rarity("REPTILOID"));
    }

    @Test void sanitizedMenuProgressClassifiesOwnedUnownedMaxedAndUnknown(){
        assertEquals(AttributeMenuParser.Ownership.UNOWNED,AttributeMenuParser.parseProgressText("Nature Elemental",List.of("Syphon 1 shard to unlock!")).ownership());
        var owned=AttributeMenuParser.parseProgressText("Berry Eater IX",List.of("Syphon 12 shards to level up!"));assertEquals(9,owned.tier());assertEquals(AttributeMenuParser.Ownership.OWNED,owned.ownership());
        assertEquals(AttributeMenuParser.Ownership.MAXED,AttributeMenuParser.parseProgressText("Veil X",List.of()).ownership());
        assertEquals(AttributeMenuParser.Ownership.UNKNOWN,AttributeMenuParser.parseProgressText("Advanced Mode",List.of()).ownership());
    }

    @Test void ownershipIsExplicit(){
        assertEquals(AttributeMenuParser.Ownership.UNOWNED,AttributeMenuParser.classifyOwnership(0,true));
        assertEquals(AttributeMenuParser.Ownership.UNKNOWN,AttributeMenuParser.classifyOwnership(0,false));
        assertEquals(AttributeMenuParser.Ownership.OWNED,AttributeMenuParser.classifyOwnership(4,false));
        assertEquals(AttributeMenuParser.Ownership.MAXED,AttributeMenuParser.classifyOwnership(10,false));
        assertTrue(AttributeMenuParser.isMissing(AttributeMenuParser.Ownership.UNOWNED));
        assertTrue(AttributeMenuParser.isMissing(AttributeMenuParser.Ownership.OWNED));
        assertFalse(AttributeMenuParser.isMissing(AttributeMenuParser.Ownership.MAXED));
    }

    @Test void progressionHandlesUnownedPartialNearMaxMaxedAndUnknownMaximum(){
        assertEquals(96,AttributeProgression.calculate(SkyblockRarity.COMMON,0,1,0).purchaseRemaining());
        assertTrue(AttributeProgression.calculate(SkyblockRarity.RARE,4,2,0).purchaseRemaining()>0);
        assertEquals(12,AttributeProgression.calculate(SkyblockRarity.RARE,9,12,0).purchaseRemaining());
        assertEquals(0,AttributeProgression.calculate(SkyblockRarity.LEGENDARY,10,-1,0).purchaseRemaining());
        assertFalse(AttributeProgression.calculate(SkyblockRarity.MYTHIC,0,1,0).known());
        assertEquals(96,AttributeProgression.maximum(SkyblockRarity.COMMON));
        assertEquals(64,AttributeProgression.maximum(SkyblockRarity.UNCOMMON));
        assertEquals(48,AttributeProgression.maximum(SkyblockRarity.RARE));
        assertEquals(32,AttributeProgression.maximum(SkyblockRarity.EPIC));
        assertEquals(24,AttributeProgression.maximum(SkyblockRarity.LEGENDARY));
    }

    @Test void menuDisplayNamesResolveWhenHypixelOmitsConcreteShardMetadata(){
        var creatureFisher=AttributeShardResolver.resolveDisplayedName("Creature Fisher II");assertNotNull(creatureFisher);assertEquals("HUNTER",creatureFisher.structuredSubtype());
        var arthropod=AttributeShardResolver.resolveDisplayedName("Arthropod Resistance");assertNotNull(arthropod);assertEquals("ARACHNO_RESISTANCE",arthropod.structuredSubtype());
        assertNull(AttributeShardResolver.resolveDisplayedName("Advanced Mode"));
        var market=HuntingShardPriceIdentity.resolveAttribute("HUNTER");assertNotNull(market);assertEquals("SHARD_SEA_ARCHER",market.bazaarName());
        assertEquals("Howling Spirit",HuntingShardPriceIdentity.resolveAttribute("ANIMAL_RESISTANCE").displayName());
        assertEquals("Howling Spirit",AttributeMenuParser.sourceName(List.of("Source: Howling Spirit")));
        assertEquals("Mantis Shrimp Shard",AttributeMenuParser.sourceName(List.of("Source: Mantis Shrimp Shard (R74)")));
        assertEquals("Lapis Zombie Shard",AttributeMenuParser.sanitizeSourceName("Lapis Zombie Shard (C12)"));
        assertEquals(320,AttributeShardHeadCatalog.size());
        assertTrue(AttributeShardHeadCatalog.has("ANIMAL_RESISTANCE"));
    }

    @Test void attributeRowsBuildFocusedBazaarSearchQueries(){
        assertEquals("Scrappy",AttributeMenuPanel.bazaarQuery("Scrappy Shard"));
        assertEquals("Mantis Shrimp",AttributeMenuPanel.bazaarQuery(" Mantis Shrimp Shard "));
        assertEquals("Howling Spirit",AttributeMenuPanel.bazaarQuery("Howling Spirit"));
    }

    @Test void attributeFilterTooltipFindsTheSelectedCategory(){
        assertEquals("Fishing",AttributeMenuPanel.selectedFilter(List.of("Filter","Combat","▶ Fishing","Farming","Click to switch filter!")));
        assertEquals("All",AttributeMenuPanel.selectedFilter(List.of("Filter","> All")));
        assertEquals("",AttributeMenuPanel.selectedFilter(List.of("Filter","Click to switch filter!")));
    }

    @Test void duplicateObservationsMergeByStableIdentity(){
        var first=row("ATTRIBUTE:NATURE_ELEMENTAL",AttributeMenuParser.Ownership.UNOWNED,0,1);
        var replacement=row("ATTRIBUTE:NATURE_ELEMENTAL",AttributeMenuParser.Ownership.OWNED,1,3);
        var target=new HashMap<String,AttributeMenuParser.Parsed>();AttributeSessionData.mergeObservations(target,List.of(first,replacement));
        assertEquals(1,target.size());assertSame(replacement,target.get("ATTRIBUTE:NATURE_ELEMENTAL"));
    }

    @Test void ownedShardChangesRecalculateCachedProgressWithoutReparsingItsIcon(){
        var cached=new AttributeMenuParser.Parsed("ATTRIBUTE:NATURE_ELEMENTAL",null,null,SkyblockRarity.COMMON,0,1,AttributeMenuParser.Ownership.UNOWNED,AttributeProgression.Result.unknown(),"Lapis Zombie Shard");
        var updated=AttributeMenuParser.withOwned(cached,7);assertNotNull(updated);assertEquals(89,updated.progress().purchaseRemaining());assertEquals("Lapis Zombie Shard",updated.sourceName());
    }

    @Test void syphonResultChatParsesPartialAndMaxedAttributeUpdates(){
        assertEquals(1,AttributeSyphonChatTracker.parseStartAmount("You used Syphon on Cavernfish Shard!"));
        assertEquals(52,AttributeSyphonChatTracker.parseStartAmount("You used Syphon on 52 Shards!"));
        assertEquals(0,AttributeSyphonChatTracker.parseStartAmount("Party > Player: You used Syphon on Cavernfish Shard!"));
        var partial=AttributeSyphonChatTracker.parseResult("+5 Honey Refill Attribute (Level 9) - 6 more to upgrade!");assertNotNull(partial);assertEquals(5,partial.amount());assertEquals("Honey Refill",partial.attributeName());assertEquals(9,partial.tier());assertEquals(6,partial.toNext());
        var screenshot=AttributeSyphonChatTracker.parseResult("+1 Cave Fishing Attribute (Level 9) - 6 more to upgrade!");assertNotNull(screenshot);assertEquals("Cave Fishing",screenshot.attributeName());assertNotNull(AttributeShardResolver.resolveDisplayedName(screenshot.attributeName()));
        var maxed=AttributeSyphonChatTracker.parseResult("§a+30 Elusive Fortune Attribute §7(Level 10) §a§lMAXED");assertNotNull(maxed);assertEquals(30,maxed.amount());assertEquals(10,maxed.tier());assertEquals(-1,maxed.toNext());
        assertNull(AttributeSyphonChatTracker.parseResult("Party > Player: I used syphon on shards"));
    }

    @Test void pageMergingRetainsKnownStateAndIgnoresTemporaryEmptyPages(){
        AttributeSessionData.ensureScope("profile-a");
        var unowned=row("ATTRIBUTE:NATURE_ELEMENTAL",AttributeMenuParser.Ownership.UNOWNED,0,1);
        var unknown=row("ATTRIBUTE:NATURE_ELEMENTAL",AttributeMenuParser.Ownership.UNKNOWN,0,-1);
        assertTrue(AttributeSessionData.observe(new AttributeMenuDetector.Page(1,2),List.of(unowned)));
        assertFalse(AttributeSessionData.observe(new AttributeMenuDetector.Page(2,2),List.of()));
        assertEquals(1,AttributeSessionData.visitedPages());
        AttributeSessionData.observe(new AttributeMenuDetector.Page(1,2),List.of(unknown));
        assertEquals(AttributeMenuParser.Ownership.UNOWNED,AttributeSessionData.progress().get("ATTRIBUTE:NATURE_ELEMENTAL").ownership());
    }

    @Test void sessionStateIsIsolatedByLifecycleScope(){
        AttributeSessionData.ensureScope("profile-one");
        AttributeSessionData.observe(new AttributeMenuDetector.Page(1,1),List.of(row("ATTRIBUTE:NATURE_ELEMENTAL",AttributeMenuParser.Ownership.UNOWNED,0,1)));
        assertEquals(1,AttributeSessionData.progress().size());
        AttributeSessionData.ensureScope("profile-two");assertTrue(AttributeSessionData.progress().isEmpty());assertEquals(0,AttributeSessionData.visitedPages());
    }

    @Test void menuPageTitlesHandleCurrentAndUnpagedForms(){
        assertEquals(new AttributeMenuDetector.Page(2,7),AttributeMenuDetector.parsePage("(2/7) Attribute Menu"));
        assertEquals(new AttributeMenuDetector.Page(1,1),AttributeMenuDetector.parsePage("Attribute Menu"));
    }

    @Test void huntingBoxPageTitlesHandleCurrentAndUnpagedForms(){
        assertEquals(new HuntingBoxMenuDetector.Page(3,7),HuntingBoxMenuDetector.parsePage("(3/7) Hunting Box"));
        assertEquals(new HuntingBoxMenuDetector.Page(1,1),HuntingBoxMenuDetector.parsePage("Hunting Box"));
    }

    @Test void hypixelBazaarSnapshotKeepsOnlyShardPrices(){
        var root=JsonParser.parseString("""
            {"success":true,"products":{
              "SHARD_ZOMBIE_SOLDIER":{"quick_status":{"buyPrice":7460.496,"sellPrice":1540.0}},
              "ENCHANTED_DIAMOND":{"quick_status":{"buyPrice":1000,"sellPrice":900}},
              "SHARD_EMPTY":{"quick_status":{"buyPrice":0,"sellPrice":0}}
            }}
            """).getAsJsonObject();
        var prices=ShardPriceService.parseProducts(root);assertEquals(1,prices.size());
        var zombie=prices.get("SHARD_ZOMBIE_SOLDIER");assertNotNull(zombie);assertEquals(7_460L,zombie.instantBuy());assertEquals(1_540L,zombie.instantSell());
        assertTrue(ShardPriceService.parseProducts(JsonParser.parseString("{\"success\":false}").getAsJsonObject()).isEmpty());
    }

    @Test void huntingBoxVirtualIdsResolveToRealMarketShardIdentities(){
        assertEquals(321,HuntingShardPriceIdentity.size());
        var zombie=HuntingShardPriceIdentity.resolve("U24","ignored");assertNotNull(zombie);
        assertEquals("SHARD_ZOMBIE_SOLDIER",zombie.bazaarName());
        assertEquals("ATTRIBUTE_SHARD_UNDEAD;1",zombie.internalName());
        assertEquals(zombie,HuntingShardPriceIdentity.resolve("","Zombie Soldier"));
        var bogged=HuntingShardPriceIdentity.resolve("C14","ignored");assertNotNull(bogged);
        assertEquals("SHARD_SEA_ARCHER",bogged.bazaarName());
    }

    private static AttributeMenuParser.Parsed row(String key,AttributeMenuParser.Ownership ownership,int tier,int toNext){
        return new AttributeMenuParser.Parsed(key,null,null,null,tier,toNext,ownership,AttributeProgression.Result.unknown(),"");
    }
}
