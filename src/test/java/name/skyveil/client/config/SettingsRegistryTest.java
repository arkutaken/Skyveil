package name.skyveil.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRegistryTest {
    @Test
    void groupsRelatedFeaturesIntoFocusedCategories(){
        SettingsRegistry.registerDefaults();

        assertEquals(java.util.List.of("interface","gameplay","hunting","pets"),SettingsRegistry.categories().stream().map(category->category.id).toList());
        ConfigCategory ui=category("interface"),gameplay=category("gameplay");
        assertEquals("general.hudEditor",ui.settings.getFirst().key);
        assertTrue(group(ui,"zoom").settings.stream().anyMatch(setting->setting.key.equals("general.zoom.key")));
        assertTrue(group(ui,"inventory_buttons").settings.size()>1);
        assertTrue(group(gameplay,"compact_damage").settings.stream().noneMatch(setting->setting.key.startsWith("general.zoom")));
        assertTrue(group(gameplay,"item_protection").settings.size()>1);
        assertFalse(group(ui,"storage_preview").description.isBlank());
    }

    private static ConfigCategory category(String id){return SettingsRegistry.categories().stream().filter(category->category.id.equals(id)).findFirst().orElseThrow();}
    private static ConfigSubcategory group(ConfigCategory category,String id){return category.subcategories.stream().filter(group->group.id.equals(id)).findFirst().orElseThrow();}
}
