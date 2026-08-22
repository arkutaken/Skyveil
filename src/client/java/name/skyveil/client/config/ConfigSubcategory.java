package name.skyveil.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigSubcategory {
    public final String id, displayName;
    public final List<SettingDefinition> settings = new ArrayList<>();
    ConfigSubcategory(String id, String displayName) { this.id=id; this.displayName=displayName; }
    public ConfigSubcategory add(SettingDefinition setting) { settings.add(setting);SettingsRegistry.changed();return this; }
}
