package name.skyveil.client.equipment;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentShortcutRowTest {
    @Test void recognizesCurrentEquipmentMenus(){assertTrue(EquipmentShortcutRow.matchesMenuTitle("Your Equipment and Stats"));assertTrue(EquipmentShortcutRow.matchesMenuTitle("(1/3) Equipment Sets"));assertTrue(EquipmentShortcutRow.matchesMenuTitle("Loadouts (1/9)"));assertFalse(EquipmentShortcutRow.matchesMenuTitle("Wardrobe (1/2)"));}
    @Test void classifiesTheFourSkyBlockEquipmentTypesFromLore(){assertEquals(0,type("LEGENDARY NECKLACE"));assertEquals(1,type("EPIC CLOAK"));assertEquals(2,type("RARE BELT"));assertEquals(3,type("MYTHIC GLOVES"));assertEquals(3,type("LEGENDARY BRACELET"));assertEquals(-1,type("LEGENDARY ACCESSORY"));}
    @Test void recognizesNewEquipmentSetPlaceholders(){assertTrue(EquipmentShortcutRow.isPlaceholderName("Slot 1 Necklace"));assertTrue(EquipmentShortcutRow.isPlaceholderName("Empty Belt Slot"));assertFalse(EquipmentShortcutRow.isPlaceholderName("Molten Necklace"));}
    private static int type(String line){return EquipmentShortcutRow.classifyLore(List.of(Component.literal(line)));}
}
