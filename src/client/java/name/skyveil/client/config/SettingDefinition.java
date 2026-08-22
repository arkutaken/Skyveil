package name.skyveil.client.config;

import java.util.function.*;

public final class SettingDefinition {
    public enum Type { TOGGLE, DECIMAL_SLIDER, COLOR, BUTTON, KEYBIND, CHOICE }
    public final String key, name, description, tooltip, buttonLabel;
    public final java.util.List<String> choices;
    public final Type type;
    public final double minimum, maximum;
    public final Supplier<Object> getter;
    public final Consumer<Object> setter;

    private SettingDefinition(String key, String name, String description, String tooltip, Type type,
                              double minimum, double maximum, Supplier<Object> getter, Consumer<Object> setter, String buttonLabel,java.util.List<String> choices) {
        this.key = key; this.name = name; this.description = description; this.tooltip = tooltip;
        this.type = type; this.minimum = minimum; this.maximum = maximum; this.getter = getter; this.setter = setter; this.buttonLabel = buttonLabel;
        this.choices=choices;
    }
    public static SettingDefinition toggle(String key, String name, String description, Supplier<Boolean> get, Consumer<Boolean> set) {
        return new SettingDefinition(key, name, description, description, Type.TOGGLE, 0, 1, () -> get.get(), v -> set.accept((Boolean)v), null,java.util.List.of());
    }
    public static SettingDefinition slider(String key, String name, String description, double min, double max, Supplier<Double> get, Consumer<Double> set) {
        return new SettingDefinition(key, name, description, description, Type.DECIMAL_SLIDER, min, max, () -> get.get(), v -> set.accept((Double)v), null,java.util.List.of());
    }
    public static SettingDefinition color(String key, String name, String description, Supplier<Integer> get, Consumer<Integer> set) {
        return new SettingDefinition(key, name, description, description, Type.COLOR, 0, 0xFFFFFF, () -> get.get(), v -> set.accept((Integer)v), null,java.util.List.of());
    }
    public static SettingDefinition button(String key, String name, String description, Runnable action) {
        return new SettingDefinition(key, name, description, description, Type.BUTTON, 0, 0, () -> null, v -> action.run(), "Open",java.util.List.of());
    }
    public static SettingDefinition keybind(String key,String name,String description,Supplier<Integer> get,Consumer<Integer> set){
        return new SettingDefinition(key,name,description,description,Type.KEYBIND,0,0,()->get.get(),v->set.accept((Integer)v),null,java.util.List.of());
    }
    public static SettingDefinition choice(String key,String name,String description,java.util.List<String> choices,Supplier<String> get,Consumer<String> set){
        return new SettingDefinition(key,name,description,description,Type.CHOICE,0,0,()->get.get(),v->set.accept((String)v),null,java.util.List.copyOf(choices));
    }
    public void set(Object value) { setter.accept(value); ConfigManager.save(); }
}
