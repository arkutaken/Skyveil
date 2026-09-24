package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PetPipelineTest {
    @Test void busyMenuStillGetsScanned(){
        assertTrue(PetTracker.deferMenuScan(400,399,0));
        assertFalse(PetTracker.deferMenuScan(1_000,999,0));
        assertFalse(PetTracker.deferMenuScan(700,100,0));
    }

    @Test void nestedMetadataPreservesHeldItemUuidAndExponentXp(){
        String json="""
            {"uuid":"aabbccdd-1111-2222-3333-123456789abc","type":"HEDGEHOG",
             "heldItem":"PET_ITEM_EXP_SHARE","exp":2.5E7,"tier":"LEGENDARY","active":true}
            """;
        var tag=new CompoundTag();var extra=new CompoundTag();
        extra.putString("petInfo",json);tag.put("ExtraAttributes",extra);
        var parsed=PetMetadata.parse(PetTracker.petMetadata(tag));
        assertEquals("PET_ITEM_EXP_SHARE",parsed.heldItem());
        assertEquals(25_000_000,parsed.xp());
        assertTrue(parsed.active());
        assertEquals(SkyblockRarity.LEGENDARY,parsed.tier());
        assertEquals(parsed.uuid(),PetInstanceId.fromMetadata(json,"HEDGEHOG","LEGENDARY","").value());
    }

    @Test void absentPetInfoCannotUseAHeadProfileUuid(){
        var tag=new CompoundTag();tag.putString("uuid","head-texture-uuid");
        assertEquals("",PetMetadata.parse(PetTracker.petMetadata(tag)).uuid());
        assertEquals("",PetMetadata.parse("{heldItem:null}").heldItem());
        assertEquals("",PetMetadata.parse("invalid").heldItem());
    }

    @Test void repeatedLoadoutSelectionsKeepEachPetsCompleteSnapshot(){
        var hedgehog=pet("one","Hedgehog","Exp Share",100);
        var skeleton=pet("two","Skeleton","Dwarf Turtle Shelmet",99);
        for(int i=0;i<25;i++){
            assertSame(hedgehog,PetTracker.selectCached(List.of(hedgehog,skeleton),"Hedgehog",100,null).getFirst());
            // Live widget colour can describe its prefix rather than the pet's rarity.
            assertSame(skeleton,PetTracker.selectCached(List.of(hedgehog,skeleton),"Skeleton",100,SkyblockRarity.COMMON).getFirst());
        }
        assertEquals("Exp Share",hedgehog.petItemName());
        assertEquals("Dwarf Turtle Shelmet",skeleton.petItemName());
    }

    @Test void duplicateOwnedPetsRemainAmbiguousWithoutIdentityEvidence(){
        var first=pet("one","Hedgehog","Exp Share",100);
        var second=pet("two","Hedgehog","Dwarf Turtle Shelmet",100);
        assertEquals(2,PetTracker.selectCached(List.of(first,second),"Hedgehog",100,null).size());
        assertTrue(PetTracker.selectCached(List.of(first,second),"Skeleton",100,null).isEmpty());
    }

    private static PetData pet(String id,String name,String item,int level){
        return new PetData(new PetInstanceId(id,PetInstanceId.Source.UUID,PetInstanceId.Confidence.EXACT),
            name.toUpperCase(),name,SkyblockRarity.LEGENDARY,level,true,100,0,0,false,level==100,
            item,item,SkyblockRarity.EPIC,0xAA00AA,ItemStack.EMPTY,ItemStack.EMPTY);
    }
}