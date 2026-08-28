package name.skyveil.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigSubcategory {
    public final String id, displayName, description;
    public final List<SettingDefinition> settings = new ArrayList<>();
    ConfigSubcategory(String id,String displayName,String description){this.id=id;this.displayName=displayName;this.description=description;}
    public ConfigSubcategory add(SettingDefinition setting) { settings.add(setting);SettingsRegistry.changed();return this; }
}
