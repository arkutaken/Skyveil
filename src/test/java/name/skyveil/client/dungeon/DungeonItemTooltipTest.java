package name.skyveil.client.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DungeonItemTooltipTest {
    @Test
    void readsQualityAndDistinguishesNormalFromMasterModeFloors() {
        assertEquals(new DungeonItemTooltip.Info("F6",40),info(6,40,"CATACOMBS:19"));
        assertEquals(new DungeonItemTooltip.Info("M6",40),info(9,40,"CATACOMBS:39"));
        assertEquals(new DungeonItemTooltip.Info("M1",50),info(4,50,"CATACOMBS:24"));
        assertEquals(new DungeonItemTooltip.Info("E",12),info(0,12,""));
    }

    @Test
    void rejectsItemsWithoutARealDungeonQualityRoll() {
        CompoundTag attributes=new CompoundTag();attributes.putInt("item_tier",6);
        assertNull(DungeonItemTooltip.inspectAttributes(attributes));
        attributes.putInt("baseStatBoostPercentage",51);
        assertNull(DungeonItemTooltip.inspectAttributes(attributes));
    }

    @Test
    void insertsTheCompactLineImmediatelyAboveRarity() {
        List<Component> decorated=DungeonItemTooltip.decorate(List.of(
            Component.literal("Rapid Machine Gun Shortbow"),
            Component.literal(""),
            Component.literal("RARE DUNGEON BOW").withStyle(ChatFormatting.BLUE),
            Component.literal("Lowest BIN: 15,000")
        ),new DungeonItemTooltip.Info("M6",40));
        assertEquals("Floor: M6  •  Quality: 40/50",decorated.get(2).getString());
        assertEquals("RARE DUNGEON BOW",decorated.get(3).getString());
    }

    private static DungeonItemTooltip.Info info(int tier,int quality,String requirement) {
        CompoundTag attributes=new CompoundTag();attributes.putInt("item_tier",tier);attributes.putInt("baseStatBoostPercentage",quality);attributes.putString("dungeon_skill_req",requirement);
        return DungeonItemTooltip.inspectAttributes(attributes);
    }
}
