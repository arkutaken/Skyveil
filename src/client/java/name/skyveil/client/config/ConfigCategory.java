package name.skyveil.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigCategory {
    public final String id, displayName;
    public final List<ConfigSubcategory> subcategories = new ArrayList<>();
    public ConfigCategory(String id, String displayName) { this.id=id; this.displayName=displayName; }
    public ConfigSubcategory add(String id, String name) { var result = new ConfigSubcategory(id, name); subcategories.add(result);SettingsRegistry.changed();return result; }
}
