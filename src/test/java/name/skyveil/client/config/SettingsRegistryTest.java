package name.skyveil.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRegistryTest {
    @Test
    void groupsRelatedFeaturesIntoFocusedCategories(){
        SettingsRegistry.registerDefaults();

        assertEquals(java.util.List.of("interface","gameplay","mining","hunting","pets","slayers"),SettingsRegistry.categories().stream().map(category->category.id).toList());
        ConfigCategory ui=category("interface"),gameplay=category("gameplay");
        assertEquals("general.hudEditor",ui.settings.getFirst().key);
        assertTrue(group(ui,"zoom").settings.stream().anyMatch(setting->setting.key.equals("general.zoom.key")));
        assertTrue(group(ui,"inventory_buttons").settings.size()>1);
        assertTrue(group(gameplay,"compact_damage").settings.stream().noneMatch(setting->setting.key.startsWith("general.zoom")));
        assertEquals(5,group(ui,"items").settings.stream().filter(setting->setting.key.startsWith("general.itemProtection.")).count());
        assertFalse(group(ui,"screen_overlays").description.isBlank());
        ConfigSubcategory voidgloom=group(category("slayers"),"voidgloom");
        assertEquals(6,voidgloom.settings.size());
        assertEquals(3,voidgloom.settings.stream().filter(setting->setting.type==SettingDefinition.Type.COLOR).count());
    }

    @Test
    void interfaceContainsMovedSettingsWithoutObsoleteGroupsOrControls(){
        SettingsRegistry.registerDefaults();
        ConfigCategory ui=category("interface");
        assertEquals(java.util.Set.of("screen_overlays","items","zoom","inventory_buttons","chat_copy"),
            ui.subcategories.stream().map(group->group.id).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("hud.inventoryPreview.opacity","hud.inventoryPreview.enabled","hud.skillXp.enabled","hud.vanilla.itemName.enabled","hud.playerStats.enabled",
            "hud.performance.enabled","general.darkMode","general.storagePreview","general.storagePreviewTheme"),
            group(ui,"screen_overlays").settings.stream().map(setting->setting.key).collect(java.util.stream.Collectors.toSet()));
        assertEquals(13,group(ui,"items").settings.size());
        assertEquals("Chat",group(ui,"chat_copy").displayName);
        assertEquals(3,group(ui,"chat_copy").settings.size());
        assertTrue(group(ui,"chat_copy").settings.stream().anyMatch(setting->setting.key.equals("bestiary.hideRewards")&&setting.type==SettingDefinition.Type.TOGGLE));
        assertFalse(ui.settings.stream().anyMatch(setting->setting.key.equals("bestiary.hideRewards")));
        assertFalse(category("gameplay").subcategories.stream().anyMatch(group->java.util.Set.of("bestiary","item_protection").contains(group.id)));
        var keys=SettingsRegistry.categories().stream().flatMap(category->java.util.stream.Stream.concat(
            category.settings.stream(),category.subcategories.stream().flatMap(group->group.settings.stream()))).map(setting->setting.key).toList();
        assertEquals(keys.size(),new java.util.HashSet<>(keys).size());
        for(String removed:java.util.List.of("hud.skillXp.layout","hud.playerStats.layout","hud.performance.scale","hud.vanilla.skillXp.dim"))
            assertFalse(keys.contains(removed),removed);
    }
    private static ConfigCategory category(String id){return SettingsRegistry.categories().stream().filter(category->category.id.equals(id)).findFirst().orElseThrow();}
    private static ConfigSubcategory group(ConfigCategory category,String id){return category.subcategories.stream().filter(group->group.id.equals(id)).findFirst().orElseThrow();}
}
