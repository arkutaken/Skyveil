package name.skyveil.client.craftcost;

import com.google.gson.JsonObject;
import name.skyveil.client.auction.AuctionTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SoulboundPricingTest {
    @Test void itemDefinitionsDistinguishInherentBindingFromMuseumEligibleItems(){
        assertTrue(CraftCostTooltip.isInherentlySoulbound("ROYAL_PIGEON"));
        assertTrue(CraftCostTooltip.isInherentlySoulbound("ARCHAEOLOGIST_COMPASS"));
        assertFalse(CraftCostTooltip.isInherentlySoulbound("TITANIUM_DRILL_4"));
        assertFalse(CraftCostTooltip.isInherentlySoulbound("HYPERION"));
        assertFalse(CraftCostTooltip.isInherentlySoulbound("ASPECT_OF_THE_END"));
        assertFalse(CraftCostTooltip.isInherentlySoulbound("UNKNOWN_ITEM"));
    }
    @Test void museumBindingDoesNotChangeReplacementCost(){
        var extra=new CompoundTag();extra.putString("id","TITANIUM_DRILL_4");
        var calculator=new CraftCostCalculator(new JsonObject(),id->100.0);
        var before=calculator.calculate(extra);
        extra.putBoolean("donated_museum",true);extra.putInt("soulbound",1);
        assertEquals(before,calculator.calculate(extra));
        assertFalse(CraftCostTooltip.isInherentlySoulbound(extra.getStringOr("id","")));
    }
    @Test void suppressedPricesRemoveCachedUnavailableRowsButPreserveItemLore(){
        var name=Component.literal("Royal Pigeon");
        var binding=Component.literal("* Co-op Soulbound *");
        List<Component> lines=List.of(name,binding,Component.literal("Full craft cost: Unavailable (unpriced inputs: ROYAL_PIGEON)"));
        assertEquals(List.of(name,binding),AuctionTooltip.removePriceLines(lines,"Full craft cost: "));
        assertEquals(List.of(name,binding),AuctionTooltip.removePriceLines(List.of(name,binding),"Full craft cost: "));
    }
}
