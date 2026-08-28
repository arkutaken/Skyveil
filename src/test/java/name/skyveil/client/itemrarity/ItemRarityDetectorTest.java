package name.skyveil.client.itemrarity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemRarityDetectorTest {
    @Test
    void readsExplicitRarityFooterFromTooltip() {
        ItemRarityDetector.Highlight highlight=detect(
            Component.literal("A powerful weapon."),
            Component.literal("RARE SWORD").withStyle(ChatFormatting.BLUE)
        );

        assertEquals(SkyblockRarity.RARE,highlight.rarity());
        assertEquals(0x5555FF,highlight.rgb());
    }

    @Test
    void supportsTwoWordRarityFooter() {
        ItemRarityDetector.Highlight highlight=detect(Component.literal("VERY SPECIAL ACCESSORY").withStyle(ChatFormatting.RED));

        assertEquals(SkyblockRarity.VERY_SPECIAL,highlight.rarity());
    }

    @Test
    void ignoresRarityWordsInsideDescriptiveLore() {
        assertNull(detect(Component.literal("Complete the experiment for a RARE reward!")));
    }

    @Test
    void usesRarityColorForDecoratedRecombobulatedFooter() {
        Component footer=Component.literal("◆ ").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal("MYTHIC CHESTPLATE").withStyle(ChatFormatting.LIGHT_PURPLE))
            .append(Component.literal(" ◆").withStyle(ChatFormatting.DARK_GRAY));
        ItemRarityDetector.Highlight highlight=detect(footer);

        assertEquals(SkyblockRarity.MYTHIC,highlight.rarity());
        assertEquals(0xFF55FF,highlight.rgb());
    }

    @Test
    void ignoresVisualLettersAndNumbersAroundColoredRarityWord() {
        Component footer=Component.literal("I ").withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.literal("MYTHIC").withStyle(ChatFormatting.LIGHT_PURPLE))
            .append(Component.literal(" SWORD e2").withStyle(ChatFormatting.DARK_PURPLE));
        ItemRarityDetector.Highlight highlight=detect(footer);

        assertEquals(SkyblockRarity.MYTHIC,highlight.rarity());
        assertEquals(0xFF55FF,highlight.rgb());
    }

    private static ItemRarityDetector.Highlight detect(Component... lines) {
        return ItemRarityDetector.detectTooltipLore(List.of(lines));
    }
}
