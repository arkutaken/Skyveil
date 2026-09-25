package name.skyveil.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigCategory {
    public final String id, displayName, description;
    // Preserve registration order for the sidebar/content view. Use add() to
    // advance the registry revision and invalidate the search index.
    public final List<SettingDefinition> settings=new ArrayList<>();
    public final List<ConfigSubcategory> subcategories = new ArrayList<>();
    public ConfigCategory(String id,String displayName,String description){this.id=id;this.displayName=displayName;this.description=description;}
    public ConfigCategory add(SettingDefinition setting){settings.add(setting);SettingsRegistry.changed();return this;}
    public ConfigSubcategory add(String id,String name,String description){var result=new ConfigSubcategory(id,name,description);subcategories.add(result);SettingsRegistry.changed();return result;}
}
