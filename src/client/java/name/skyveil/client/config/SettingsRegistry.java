package name.skyveil.client.config;

import java.util.*;

public final class SettingsRegistry {
    private static final List<ConfigCategory> CATEGORIES = new ArrayList<>();
    private static long revision;
    private SettingsRegistry() {}
    public static List<ConfigCategory> categories() { return Collections.unmodifiableList(CATEGORIES); }
    public static long revision(){return revision;}
    static void changed(){revision++;}
    public static ConfigCategory register(String id, String name) { var c=new ConfigCategory(id,name); CATEGORIES.add(c);changed();return c; }

    public static void registerDefaults() {
        if (!CATEGORIES.isEmpty()) return;
        ConfigCategory general=register("general", "General");
        general.add("general", "General")
            .add(SettingDefinition.button("general.hudEditor", "Relocate & Resize HUDs", "Relocate and resize currently enabled HUD features.", name.skyveil.client.gui.HudEditorScreen::open))
            .add(SettingDefinition.toggle("general.scrollableTooltips","Scrollable Tooltips","Allows oversized item tooltips to be scrolled with the mouse wheel.",()->ConfigManager.get().scrollableTooltips,v->ConfigManager.get().scrollableTooltips=v))
            .add(SettingDefinition.toggle("general.compactDamage","Compact Damage","Replaces rapid damage indicators with one moving five-hit average above the target.",()->ConfigManager.get().compactDamage,v->{ConfigManager.get().compactDamage=v;if(!v)name.skyveil.client.combat.CompactDamageManager.clear();}))
            .add(SettingDefinition.toggle("general.zoom.enabled","Zoom","Hold the Zoom Key to magnify the world without a spyglass scope.",()->ConfigManager.get().zoom.enabled,v->ConfigManager.get().zoom.enabled=v))
            .add(SettingDefinition.keybind("general.zoom.key","Zoom Key","Hold to zoom. Scroll up while held to zoom in and scroll down to zoom out.",()->ConfigManager.get().zoom.key,v->ConfigManager.get().zoom.key=v))
            .add(SettingDefinition.choice("general.darkMode","Dark Mode","Changes the appearance of Minecraft inventory and container GUIs.",List.of("Default","Dark","Dark Purple"),
                ()->switch(ConfigManager.get().darkMode){case "DARK"->"Dark";case "DARK_PURPLE"->"Dark Purple";default->"Default";},
                value->ConfigManager.get().darkMode=switch(value){case "Dark"->"DARK";case "Dark Purple"->"DARK_PURPLE";default->"DEFAULT";}));
        general.add("storage_preview","Storage Preview")
            .add(SettingDefinition.toggle("general.storagePreview","Storage Preview","Turns owned Ender Chest and Backpack previews into a persistent storage workspace beside the player inventory. Cached contents persist between game sessions.",()->ConfigManager.get().storagePreview,v->ConfigManager.get().storagePreview=v))
            .add(SettingDefinition.choice("general.storagePreviewTheme","Storage Preview Color","Changes only the Storage Preview appearance.",List.of("Default","Dark","Dark Purple"),
                ()->switch(ConfigManager.get().storagePreviewTheme){case "DARK"->"Dark";case "DARK_PURPLE"->"Dark Purple";default->"Default";},
                value->ConfigManager.get().storagePreviewTheme=switch(value){case "Dark"->"DARK";case "Dark Purple"->"DARK_PURPLE";default->"DEFAULT";}));
        general.add("item_protection","Item Protection")
            .add(SettingDefinition.toggle("general.itemProtection.enabled","Enable Item Protection","Prevents locked items from being moved or dropped.",()->ConfigManager.get().itemProtection.enabled,v->ConfigManager.get().itemProtection.enabled=v))
            .add(SettingDefinition.keybind("general.itemProtection.key","Item Lock / Link Key","Tap while hovering to lock or unlock. Hold it and left-click a hotbar slot plus one or more inventory slots to link them; select an existing pair again to unlink.",()->ConfigManager.get().itemProtection.lockKey,v->ConfigManager.get().itemProtection.lockKey=v))
            .add(SettingDefinition.toggle("general.itemProtection.icon","Show Lock Icon","Shows a lock mark over protected items.",()->ConfigManager.get().itemProtection.showLockIcon,v->ConfigManager.get().itemProtection.showLockIcon=v))
            .add(SettingDefinition.slider("general.itemProtection.opacity","Lock Icon Opacity","Changes the transparency of lock marks.",.25,1.0,()->ConfigManager.get().itemProtection.lockIconOpacity,v->ConfigManager.get().itemProtection.lockIconOpacity=v))
            .add(SettingDefinition.toggle("general.itemProtection.feedback","Locked Item Feedback","Shows a short message when an item action is blocked.",()->ConfigManager.get().itemProtection.feedback,v->ConfigManager.get().itemProtection.feedback=v));
        general.add("item_rarity","Item Rarity")
            .add(SettingDefinition.toggle("general.itemRarity.enabled","Item Rarity Backgrounds","Shows the appropriate rarity-colored background behind supported SkyBlock items.",()->ConfigManager.get().itemRarity.enabled,v->ConfigManager.get().itemRarity.enabled=v));
        general.add("inventory_buttons","Inventory Buttons")
            .add(SettingDefinition.button("general.inventoryButtons.manage","Button Management","Opens your inventory with the surrounding button editor visible.",name.skyveil.client.inventorybuttons.InventoryButtonManagementScreen::open))
            .add(SettingDefinition.toggle("general.inventoryButtons.enabled","Inventory Buttons","Shows saved command buttons around inventory screens.",()->ConfigManager.get().inventoryButtons.enabled,v->ConfigManager.get().inventoryButtons.enabled=v))
            .add(SettingDefinition.toggle("general.inventoryButtons.potionEffects","Show Potion Effects","Shows the vanilla status-effect panel beside the player inventory. Disabled by default.",()->ConfigManager.get().inventoryButtons.showPotionEffects,v->ConfigManager.get().inventoryButtons.showPotionEffects=v))
            .add(SettingDefinition.slider("general.inventoryButtons.scale","Button Scale","Changes inventory button size while keeping positions anchored to the GUI.",.75,1.25,()->ConfigManager.get().inventoryButtons.scale,v->ConfigManager.get().inventoryButtons.scale=v))
            .add(SettingDefinition.toggle("general.inventoryButtons.tooltips","Show Button Tooltips","Shows commands and click controls when hovered.",()->ConfigManager.get().inventoryButtons.showTooltips,v->ConfigManager.get().inventoryButtons.showTooltips=v))
            .add(SettingDefinition.toggle("general.inventoryButtons.containers","Show In Containers","Keeps saved Inventory Buttons available around Auction House, Bazaar, Accessory Bag, Storage, and other container screens.",()->ConfigManager.get().inventoryButtons.showInContainers,v->ConfigManager.get().inventoryButtons.showInContainers=v));
        general.add("keybinds","Keybinds")
            .add(SettingDefinition.toggle("general.wardrobe.numberKeys","Wardrobe Number Keys","While the Wardrobe menu is open, press number keys to equip the matching Wardrobe slot.",()->ConfigManager.get().wardrobe.numberKeys,v->ConfigManager.get().wardrobe.numberKeys=v))
            .add(SettingDefinition.toggle("general.customKeybinds.enabled","Custom Command Keybinds","Enables or disables every saved custom command keybind without deleting them.",()->ConfigManager.get().customKeybinds.enabled,v->ConfigManager.get().customKeybinds.enabled=v))
            .add(SettingDefinition.button("general.customKeybinds.manage","Manage Keybinds","Add, edit, enable, disable, or delete custom command keybinds.",name.skyveil.client.customkeybind.CustomKeybindManagementScreen::open));
        ConfigCategory hunting=register("hunting","Hunting");
        hunting.add("general","General")
            .add(SettingDefinition.toggle("hunting.huntingBoxValue","Hunting Box Value Panel","Shows owned shard quantities and current Bazaar sell values beside the Hunting Box.",()->ConfigManager.get().hunting.huntingBoxValue,v->ConfigManager.get().hunting.huntingBoxValue=v))
            .add(SettingDefinition.toggle("hunting.attributeProgress","Attribute Progress Panel","Shows collection progress and missing Attribute Shards beside the Attribute Menu.",()->ConfigManager.get().hunting.attributeProgress,v->ConfigManager.get().hunting.attributeProgress=v))
            .add(SettingDefinition.toggle("hunting.attributePricing","Attribute Shard Prices","Adds current Bazaar acquisition prices and required-cost totals to the Attribute Menu panel.",()->ConfigManager.get().hunting.attributePricing,v->ConfigManager.get().hunting.attributePricing=v));
        ConfigCategory bestiary=register("bestiary","Bestiary");
        bestiary.add("general","General")
            .add(SettingDefinition.toggle("bestiary.hideRewards","Hide Bestiary Rewards","Keeps Bestiary tier-up messages visible while hiding their reward section from chat.",()->ConfigManager.get().bestiary.hideRewards,v->ConfigManager.get().bestiary.hideRewards=v));
        ConfigCategory map=register("map","Map");
        map.add("general","General")
            .add(SettingDefinition.toggle("map.enabled","Enable Map System","Enables the minimap and large map features.",()->ConfigManager.get().map.enabled,v->ConfigManager.get().map.enabled=v))
            .add(SettingDefinition.keybind("map.largeMapKey","Large Map Keybind","Opens or closes the large map.",()->ConfigManager.get().map.largeMapKey,v->ConfigManager.get().map.largeMapKey=v));
        map.add("minimap","Minimap")
            .add(SettingDefinition.toggle("map.minimap.enabled","Enable Minimap","Shows the player-centered map HUD for supported islands.",()->ConfigManager.get().map.minimapEnabled,v->ConfigManager.get().map.minimapEnabled=v))
            .add(SettingDefinition.slider("map.minimap.size","Minimap Size","Changes the base map size in GUI pixels.",80,220,()->ConfigManager.get().map.minimapSize,v->ConfigManager.get().map.minimapSize=v))
            .add(SettingDefinition.slider("map.minimap.zoom","Minimap Zoom","Sets how many world blocks each map pixel represents.",1,8,()->ConfigManager.get().map.minimapZoom,v->ConfigManager.get().map.minimapZoom=v))
            .add(SettingDefinition.slider("map.minimap.opacity","Minimap Opacity","Changes the map background opacity.",.20,1,()->ConfigManager.get().map.minimapOpacity,v->ConfigManager.get().map.minimapOpacity=v))
            .add(SettingDefinition.toggle("map.minimap.direction","Show Player Direction","Shows the direction the player is facing.",()->ConfigManager.get().map.minimapDirection,v->ConfigManager.get().map.minimapDirection=v))
            .add(SettingDefinition.toggle("map.minimap.npcs","Show NPC Markers","Shows verified NPC points on the minimap.",()->ConfigManager.get().map.minimapNpcs,v->ConfigManager.get().map.minimapNpcs=v))
            .add(SettingDefinition.toggle("map.minimap.custom","Show Custom Markers","Shows your saved markers on the minimap.",()->ConfigManager.get().map.minimapCustom,v->ConfigManager.get().map.minimapCustom=v));
        map.add("large_map","Large Map")
            .add(SettingDefinition.slider("map.large.scale","Large Map Scale","Changes how much of the screen the map uses.",.50,1,()->ConfigManager.get().map.largeMapScale,v->ConfigManager.get().map.largeMapScale=v))
            .add(SettingDefinition.toggle("map.large.player","Show Player Marker","Shows your live position on the large map.",()->ConfigManager.get().map.largePlayer,v->ConfigManager.get().map.largePlayer=v))
            .add(SettingDefinition.toggle("map.large.coordinates","Show Player Coordinates","Shows your current X, Y, and Z.",()->ConfigManager.get().map.largeCoordinates,v->ConfigManager.get().map.largeCoordinates=v))
            .add(SettingDefinition.toggle("map.large.npcs","Show NPCs","Shows verified NPC markers.",()->ConfigManager.get().map.largeNpcs,v->ConfigManager.get().map.largeNpcs=v))
            .add(SettingDefinition.toggle("map.large.zones","Show Zone Names","Shows verified zone center labels, not fabricated region borders.",()->ConfigManager.get().map.largeZones,v->ConfigManager.get().map.largeZones=v))
            .add(SettingDefinition.toggle("map.large.custom","Show Custom Markers","Shows your saved coordinate markers.",()->ConfigManager.get().map.largeCustom,v->ConfigManager.get().map.largeCustom=v));
        map.add("npcs","NPCs")
            .add(SettingDefinition.toggle("map.npcs.enabled","Show NPCs","Master switch for all map NPC markers.",()->ConfigManager.get().map.npcsEnabled,v->ConfigManager.get().map.npcsEnabled=v))
            .add(SettingDefinition.toggle("map.npcs.minimap","Show NPCs on Minimap","Shows nearby NPC heads inside the visible minimap crop.",()->ConfigManager.get().map.minimapNpcs,v->ConfigManager.get().map.minimapNpcs=v))
            .add(SettingDefinition.toggle("map.npcs.large","Show NPCs on Large Map","Shows NPC heads on the stationary large map.",()->ConfigManager.get().map.largeNpcs,v->ConfigManager.get().map.largeNpcs=v))
            .add(SettingDefinition.toggle("map.npcs.tooltips","Show NPC Tooltips","Shows verified names and descriptions when hovering NPC heads.",()->ConfigManager.get().map.npcTooltips,v->ConfigManager.get().map.npcTooltips=v))
            .add(SettingDefinition.toggle("map.npcs.tooltipCoordinates","Show NPC Coordinates","Adds X, Y, and Z to NPC hover tooltips.",()->ConfigManager.get().map.npcTooltipCoordinates,v->ConfigManager.get().map.npcTooltipCoordinates=v))
            .add(SettingDefinition.slider("map.npcs.size","NPC Marker Size","Changes NPC face size from 8 to 20 GUI pixels.",8,20,()->ConfigManager.get().map.npcMarkerSize,v->ConfigManager.get().map.npcMarkerSize=v))
            .add(SettingDefinition.slider("map.player.size","Player Marker Size","Changes your player-face marker size from 8 to 20 GUI pixels.",8,20,()->ConfigManager.get().map.playerMarkerSize,v->ConfigManager.get().map.playerMarkerSize=v));
        map.add("markers","Markers")
            .add(SettingDefinition.button("map.markers.manage","Manage Map Markers","Open the large map, then left-click a marker to edit it or right-click the map to add one.",name.skyveil.client.map.LargeMapScreen::open))
            .add(SettingDefinition.toggle("map.debug","Map Calibration Debug","Shows coordinate grid and calibration data. Intended for development only.",()->ConfigManager.get().map.debug,v->ConfigManager.get().map.debug=v));
        ConfigCategory pets=register("pets","Pets");
        pets.add("pet_display","Pet Display")
            .add(SettingDefinition.toggle("hud.petDisplay.enabled","Pet Display","Shows the currently equipped SkyBlock pet and its synchronized level data.",()->ConfigManager.get().petDisplay.enabled,v->ConfigManager.get().petDisplay.enabled=v))
            .add(SettingDefinition.slider("hud.petDisplay.opacity","Background Opacity","Controls the purple panel opacity.",0,1,()->ConfigManager.get().petDisplay.backgroundOpacity,v->ConfigManager.get().petDisplay.backgroundOpacity=v))
            .add(SettingDefinition.slider("hud.petDisplay.scale","Pet Display Scale","Uniformly scales the panel, text, icons, and progress bar.",.5,2,()->ConfigManager.get().petDisplay.scale,v->ConfigManager.get().petDisplay.scale=v))
            .add(SettingDefinition.toggle("hud.petDisplay.progress","Show XP Progress Bar","Shows progress through the pet's current level.",()->ConfigManager.get().petDisplay.showProgressBar,v->ConfigManager.get().petDisplay.showProgressBar=v))
            .add(SettingDefinition.toggle("hud.petDisplay.item","Show Pet Item","Shows the item attached to the equipped pet.",()->ConfigManager.get().petDisplay.showPetItem,v->ConfigManager.get().petDisplay.showPetItem=v))
            .add(SettingDefinition.toggle("pets.hideAutoPetRuleMessage","Hide Auto Pet Rule Message","Hides only Hypixel's Autopet equipped-your-pet notification.",()->ConfigManager.get().petDisplay.hideAutoPetRuleMessage,v->ConfigManager.get().petDisplay.hideAutoPetRuleMessage=v));
    }
}
