package name.skyveil.client.pet;

import name.skyveil.client.itemrarity.SkyblockRarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetCacheIdentityTest {
    @Test
    void autoPetMessageWithoutLevelOrRarityReusesDetailedCachedPet() {
        assertTrue(matches("Tarantula",48,true,SkyblockRarity.LEGENDARY,"Tarantula",-1,null));
    }

    @Test
    void suppliedIdentitySignalsStillRejectTheWrongPet() {
        assertFalse(matches("Tarantula",48,true,SkyblockRarity.LEGENDARY,"Tarantula",49,SkyblockRarity.LEGENDARY));
        assertFalse(matches("Tarantula",48,true,SkyblockRarity.LEGENDARY,"Tarantula",48,SkyblockRarity.EPIC));
        assertFalse(matches("Tarantula",48,true,SkyblockRarity.LEGENDARY,"Griffin",48,SkyblockRarity.LEGENDARY));
    }

    @Test
    void mutableMetadataDoesNotCreateAnotherPetIdentity(){
        var first=PetInstanceId.fromMetadata("{type:ELEPHANT,exp:50,heldItem:OLD,active:false}","ELEPHANT","LEGENDARY","OLD");
        var updated=PetInstanceId.fromMetadata("{type:ELEPHANT,exp:500,heldItem:NEW,active:true}","ELEPHANT","LEGENDARY","NEW");
        org.junit.jupiter.api.Assertions.assertEquals(first,updated);
        org.junit.jupiter.api.Assertions.assertNotEquals(first,first.disambiguated(1));
    }

    @Test
    void uniquePetKeepsMenuDetailsAfterLevellingAndIgnoresLivePlaceholders(){
        var id=PetInstanceId.fromMetadata("","ELEPHANT","LEGENDARY","");
        var pet=new PetData(id,"ELEPHANT","Elephant",SkyblockRarity.LEGENDARY,48,true,100,
            0,100,true,false,"EXP_SHARE","Exp Share",null,0,
            net.minecraft.world.item.ItemStack.EMPTY,net.minecraft.world.item.ItemStack.EMPTY);
        var unresolved=new PetData(new PetInstanceId("unresolved:elephant",PetInstanceId.Source.LIVE_UNRESOLVED,PetInstanceId.Confidence.UNKNOWN),
            "","Elephant",SkyblockRarity.LEGENDARY,49,true,100,0,0,false,false,"","",null,0,
            net.minecraft.world.item.ItemStack.EMPTY,net.minecraft.world.item.ItemStack.EMPTY);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(pet),
            PetTracker.selectCached(java.util.List.of(pet,unresolved),"Elephant",49,SkyblockRarity.LEGENDARY));
    }
    @Test
    void readsNestedPetInfoWithoutSnbtEscaping(){
        var tag=new net.minecraft.nbt.CompoundTag();
        var attributes=new net.minecraft.nbt.CompoundTag();
        String info="""
            {"type":"SKELETON","heldItem":"PET_ITEM_EXP_SHARE","tier":"LEGENDARY"}
            """.trim();
        attributes.putString("petInfo",info);tag.put("ExtraAttributes",attributes);
        org.junit.jupiter.api.Assertions.assertEquals(info,PetTracker.petMetadata(tag));
        org.junit.jupiter.api.Assertions.assertNotEquals(
            PetInstanceId.fromMetadata("","skeleton","",""),
            PetInstanceId.fromMetadata("","elephant","",""));
    }
    private static boolean matches(String cachedName,int cachedLevel,boolean cachedLevelKnown,SkyblockRarity cachedRarity,String name,int level,SkyblockRarity rarity) {
        return PetTracker.matchesCachedIdentity(cachedName,cachedLevel,cachedLevelKnown,cachedRarity,name,level,rarity);
    }
}
