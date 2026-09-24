package name.skyveil.client.itemprotection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProtectedItemTest {
    @Test void uuidSurvivesUpgradeAndDistinguishesIdenticalWeapons(){
        var data=new CompoundTag();var extra=new CompoundTag();extra.putString("uuid","AAA");extra.putString("id","SWORD");data.put("ExtraAttributes",extra);
        String first=ProtectedItemManager.key(data,"minecraft:diamond_sword");
        extra.putString("id","UPGRADED_SWORD");
        assertEquals(first,ProtectedItemManager.key(data,"minecraft:diamond_sword"));
        extra.putString("uuid","BBB");
        assertNotEquals(first,ProtectedItemManager.key(data,"minecraft:diamond_sword"));
    }
    @Test void onlyDropActionsAreBlocked(){
        assertTrue(ProtectedItemManager.drops(ContainerInput.THROW,5));
        assertTrue(ProtectedItemManager.drops(ContainerInput.PICKUP,-999));
        assertFalse(ProtectedItemManager.drops(ContainerInput.PICKUP,5));
        assertFalse(ProtectedItemManager.drops(ContainerInput.QUICK_MOVE,5));
        assertFalse(ProtectedItemManager.drops(ContainerInput.SWAP,5));
        assertFalse(ProtectedItemManager.drops(ContainerInput.PICKUP_ALL,5));
        assertFalse(ProtectedItemManager.drops(ContainerInput.QUICK_CRAFT,-999));
    }
}