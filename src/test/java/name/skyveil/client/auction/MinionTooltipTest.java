package name.skyveil.client.auction;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MinionTooltipTest {
    @Test void onlyTieredMinionIdsUseCraftCostOnlyPricing(){
        assertTrue(AuctionTooltip.isMinion("COBBLESTONE_GENERATOR_1"));
        assertTrue(AuctionTooltip.isMinion("INFERNO_GENERATOR_12"));
        assertFalse(AuctionTooltip.isMinion("MINION_EXPANDER"));
        assertFalse(AuctionTooltip.isMinion("TITANIUM_DRILL_4"));
        assertFalse(AuctionTooltip.isMinion("COBBLESTONE_GENERATOR_0"));
    }
    @Test void reusedTooltipRetainsCraftCostAndLoreButDropsEveryMarketRow(){
        var lore=Component.literal("Cobblestone Minion XI");
        var craft=Component.literal("Full craft cost: 123");
        List<Component> lines=List.of(lore,Component.literal("Lowest BIN: 1"),
            Component.literal("3-day average: 2"),Component.literal("Active BIN average: 3"),
            Component.literal("Insta Buy: 4"),Component.literal("Insta Sell: 5"),craft);
        assertEquals(List.of(lore,craft),AuctionTooltip.craftCostOnly(lines));
    }
    @org.junit.jupiter.api.Test void repeatedPriceRefreshDoesNotGrowTooltip(){
        // Cached rows pass through decorators repeatedly while an inventory stays open.
        java.util.List<net.minecraft.network.chat.Component> lines=java.util.List.of(net.minecraft.network.chat.Component.literal("Item"));
        for(int i=0;i<100;i++){
            lines=AuctionTooltip.removePriceLines(lines,"Lowest BIN: ");
            lines=AuctionTooltip.replacePriceLine(lines,"Lowest BIN: ",net.minecraft.network.chat.Component.literal("Lowest BIN: "+i));
        }
        org.junit.jupiter.api.Assertions.assertEquals(3,lines.size());
        org.junit.jupiter.api.Assertions.assertEquals("Lowest BIN: 99",lines.getLast().getString());
    }}
