package name.skyveil.client.itemsearch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemSearchCatalogTest {
    @Test void bundledCatalogSearchesNamesAndStaysInactiveForBlankInput(){
        assertEquals(8_746,ItemSearchCatalog.size());assertTrue(ItemSearchCatalog.search("   ").isEmpty());
        var cavernfish=ItemSearchCatalog.search("cavernfish shard");assertFalse(cavernfish.isEmpty());assertEquals("ATTRIBUTE_SHARD_CAVE_FISHING;1",cavernfish.getFirst().internalName());assertEquals("Cavernfish Shard",cavernfish.getFirst().name());
        var giant=ItemSearchCatalog.search("giant's sword");assertFalse(giant.isEmpty());assertTrue(giant.getFirst().name().toLowerCase(java.util.Locale.ROOT).contains("giant"));
        var terminator=ItemSearchCatalog.search("terminator");assertFalse(terminator.isEmpty());assertEquals("TERMINATOR",terminator.getFirst().internalName());assertTrue(terminator.getFirst().craftable());assertEquals("Terminator",terminator.getFirst().recipeQuery());
        assertFalse(ItemSearchCatalog.search("zombie monster").getFirst().craftable());
    }

    @Test void catalogReconstructsHeadsBuiltInModelsAndHoverLore(){
        var head=ItemSearchCatalog.search("6th anniversary barn skin").getFirst();assertEquals("minecraft:skull",head.baseId());assertTrue(head.hasHeadTexture());assertTrue(head.loreLines()>0);
        var modeled=ItemSearchCatalog.search("abiphone x red").getFirst();assertEquals("hypixel_skyblock:item/abiphones/x/abiphone_x_red",modeled.modelId());assertTrue(modeled.loreLines()>0);
    }

    @Test void catalogTooltipTextIsNeverImplicitlyItalic(){
        var component=ItemSearchCatalog.legacy("§7Damage: §c+310");
        assertFalse(component.getSiblings().isEmpty());
        for(var sibling:component.getSiblings())assertEquals(Boolean.FALSE,sibling.getStyle().isItalic());
    }

    @Test void lorePrefixMatchesCurrentStackTooltipTextInsteadOfCatalog(){
        String query="  LORE: wither impact";
        assertTrue(ItemSearchCatalog.isLoreSearch(query));
        assertTrue(ItemSearchCatalog.search("lore:").isEmpty());
        assertTrue(ItemSearchCatalog.search(query).isEmpty());
        assertTrue(ItemSearchCatalog.matchesLore(java.util.List.of(net.minecraft.network.chat.Component.literal("Ability: Wither Impact RIGHT CLICK")),query));
        assertFalse(ItemSearchCatalog.matchesLore(java.util.List.of(net.minecraft.network.chat.Component.literal("Ability: Shadow Warp RIGHT CLICK")),query));
    }

    @Test void inventoryButtonHeadCatalogExposesPersistentSearchableKeys(){
        assertEquals(2_535,SkyBlockHeadIcons.all().size());
        var badphone=SkyBlockHeadIcons.all().stream().filter(icon->icon.name().equals("Maddox Badphone")).findFirst().orElseThrow();
        assertTrue(SkyBlockHeadIcons.isKey(badphone.key()));
        assertTrue(SkyBlockHeadIcons.exists(badphone.key()));
        assertEquals("AATROX_BADPHONE",badphone.internalName());
        assertFalse(SkyBlockHeadIcons.exists(SkyBlockHeadIcons.PREFIX+"6_ANNIVERSARY_BARN_SKIN"));
        assertTrue(SkyBlockHeadIcons.all().stream().noneMatch(icon->icon.name().toLowerCase(java.util.Locale.ROOT).matches(".*\\bskin\\b.*")));
        var bees=SkyBlockHeadIcons.all().stream().filter(icon->icon.internalName().startsWith("BEE;")).toList();
        assertEquals(1,bees.size());assertEquals("BEE;5",bees.getFirst().internalName());
        var acaciaMinions=SkyBlockHeadIcons.all().stream().filter(icon->icon.internalName().startsWith("ACACIA_GENERATOR_")).toList();
        assertEquals(1,acaciaMinions.size());assertEquals("ACACIA_GENERATOR_12",acaciaMinions.getFirst().internalName());
        assertEquals(520,SkyBlockHeadIcons.npcs().size());
        assertTrue(SkyBlockHeadIcons.npcs().stream().anyMatch(icon->icon.internalName().equals("ADVENTURER_NPC")));
        assertTrue(SkyBlockHeadIcons.all().stream().anyMatch(icon->icon.internalName().equals("ADVENTURER_NPC")));
        for(String removed:new String[]{"ABICASE_BLUE_AQUA","DYE_AQUAMARINE","ADAPTIVE_BELT","ADVENT_CALENDAR_DISPLAY"})assertFalse(SkyBlockHeadIcons.exists(SkyBlockHeadIcons.PREFIX+removed));
    }

    @Test void auctionTitlesAreRecognizedForLoreQueryPreservation(){
        assertTrue(ItemSearchOverlay.isAuctionTitle("Auctions Browser"));
        assertTrue(ItemSearchOverlay.isAuctionTitle("Manage Auctions"));
        assertFalse(ItemSearchOverlay.isAuctionTitle("Large Chest"));
    }
}
