package name.skyveil.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Defines the user-facing hierarchy used by the Skyveil settings workspace. */
public final class SettingsRegistry {
    private static final List<ConfigCategory> CATEGORIES=new ArrayList<>();
    private static long revision;
    private SettingsRegistry(){}
    public static List<ConfigCategory> categories(){return Collections.unmodifiableList(CATEGORIES);}
    public static long revision(){return revision;}
    static void changed(){revision++;}
    public static ConfigCategory register(String id,String name,String description){var category=new ConfigCategory(id,name,description);CATEGORIES.add(category);changed();return category;}

    // Register once. Keys are stable navigation identifiers; labels and descriptions
    // may change without breaking HUD right-click targets.
    public static void registerDefaults(){
        if(!CATEGORIES.isEmpty())return;

        ConfigCategory ui=register("interface","Interface","Menus, overlays, inventory tools, and visual presentation.");
        ui.add(SettingDefinition.button("general.hudEditor","HUD Layout","Move and resize the Skyveil overlays currently enabled on screen.",name.skyveil.client.gui.HudEditorScreen::open));
        ConfigSubcategory overlays=ui.add("screen_overlays","Screen Overlays","HUD modules, screen appearance, and storage previews.");
        overlays.add(SettingDefinition.toggle("hud.inventoryPreview.enabled","Enable Inventory Preview","Shows your live inventory above the stat bars. Move and resize it in HUD Layout.",()->ConfigManager.get().inventoryPreview.enabled,v->ConfigManager.get().inventoryPreview.enabled=v));
        overlays.add(SettingDefinition.slider("hud.inventoryPreview.opacity","Inventory Preview Opacity","Background opacity: 0 is transparent and 1 is opaque.",0,1,()->ConfigManager.get().inventoryPreview.backgroundOpacity,v->ConfigManager.get().inventoryPreview.backgroundOpacity=v));
        ConfigSubcategory items=ui.add("items","Items","Tooltips, item rarity, and item protection.");
        overlays
            .add(SettingDefinition.toggle("hud.skillXp.enabled","Enable Skill XP Display","Moves skill XP out of the default action bar into its own HUD. Disable to restore the vanilla position.",()->ConfigManager.get().skillXp.enabled,v->ConfigManager.get().skillXp.enabled=v));
        overlays
            .add(SettingDefinition.toggle("hud.vanilla.itemName.enabled","Show Switched Item Name","Shows the selected item's name when switching hotbar slots in SkyBlock. The name is dimmed in inventory.",()->ConfigManager.get().showSwitchedItemName,v->ConfigManager.get().showSwitchedItemName=v));
        overlays
            .add(SettingDefinition.toggle("hud.playerStats.enabled","Enable Player Stats","Replaces hearts, hunger, and action-bar stats. Move and resize each display in HUD Layout.",()->ConfigManager.get().playerStats.enabled,v->ConfigManager.get().playerStats.enabled=v));
        overlays
            .add(SettingDefinition.toggle("hud.performance.enabled","Enable Performance Display","Shows FPS, ping, and TPS in the top-left corner by default.",()->ConfigManager.get().performance.enabled,v->ConfigManager.get().performance.enabled=v));
        overlays
            .add(SettingDefinition.choice("general.darkMode","Dark Mode","Changes the appearance of Minecraft inventory and container GUIs.",List.of("Default","Dark"),
                ()->switch(ConfigManager.get().darkMode){case "DARK"->"Dark";default->"Default";},
                value->ConfigManager.get().darkMode=switch(value){case "Dark"->"DARK";default->"DEFAULT";}));
        items.add(SettingDefinition.toggle("general.scrollableTooltips","Scrollable Tooltips","Allows oversized item tooltips to be scrolled with the mouse wheel.",()->ConfigManager.get().scrollableTooltips,v->ConfigManager.get().scrollableTooltips=v));
        items.add(SettingDefinition.toggle("general.fullCraftCost","Full Craft Cost","Estimate recipe ingredients plus applied upgrades at current buy prices. Missing costs show Unavailable; XP, time, and random reforge rolls are not priced.",()->ConfigManager.get().fullCraftCost,v->ConfigManager.get().fullCraftCost=v));
        items.add(SettingDefinition.toggle("general.auctionTooltip","Auction House Item Prices","Show lowest BIN and a rolling 3-day average. Lowest BIN compares base items; the 3-day estimate requires matching upgrades on active listings.",()->ConfigManager.get().auctionTooltip,v->ConfigManager.get().auctionTooltip=v));
        items.add(SettingDefinition.toggle("general.bazaarTooltip","Bazaar Item Prices","Show current per-item Insta Buy and Insta Sell prices for Bazaar products.",()->ConfigManager.get().bazaarTooltip,v->ConfigManager.get().bazaarTooltip=v));
        items.add(SettingDefinition.toggle("general.dropProtection.enabled","Protect Items From Dropping","Protection follows each item's UUID and allows inventory/storage movement. Items without UUIDs are protected by type.",()->ConfigManager.get().itemProtection.protectItems,v->ConfigManager.get().itemProtection.protectItems=v));
        items.add(SettingDefinition.keybind("general.dropProtection.key","Protect Item Key","Press while hovering an item to toggle drop protection. Slot locks remain separate.",()->ConfigManager.get().itemProtection.protectItemKey,v->ConfigManager.get().itemProtection.protectItemKey=v));
        ui.add("zoom","Zoom","Configure the hold-to-zoom camera and its activation key.")
            .add(SettingDefinition.toggle("general.zoom.enabled","Enable Zoom","Hold the Zoom Key to magnify the world without a spyglass scope.",()->ConfigManager.get().zoom.enabled,v->ConfigManager.get().zoom.enabled=v))
            .add(SettingDefinition.keybind("general.zoom.key","Zoom Key","Hold to zoom. Scroll while held to change magnification.",()->ConfigManager.get().zoom.key,v->ConfigManager.get().zoom.key=v));
        overlays
            .add(SettingDefinition.toggle("general.storagePreview","Enable Storage Preview","Turns owned Ender Chest and Backpack previews into a persistent storage workspace beside the player inventory.",()->ConfigManager.get().storagePreview,v->ConfigManager.get().storagePreview=v))
            .add(SettingDefinition.choice("general.storagePreviewTheme","Storage Preview Color","Changes only the Storage Preview appearance.",List.of("Default","Dark"),
                ()->switch(ConfigManager.get().storagePreviewTheme){case "DARK"->"Dark";default->"Default";},
                value->ConfigManager.get().storagePreviewTheme=switch(value){case "Dark"->"DARK";default->"DEFAULT";}));
        ui.add("inventory_buttons","Inventory Buttons","Create and customize command buttons surrounding inventory menus.")
            .add(SettingDefinition.toggle("general.inventoryButtons.enabled","Enable Inventory Buttons","Shows saved command buttons around inventory screens.",()->ConfigManager.get().inventoryButtons.enabled,v->ConfigManager.get().inventoryButtons.enabled=v))
            .add(SettingDefinition.button("general.inventoryButtons.manage","Manage Buttons","Opens your inventory with the surrounding button editor visible.",name.skyveil.client.inventorybuttons.InventoryButtonManagementScreen::open))
            .add(SettingDefinition.toggle("general.inventoryButtons.containers","Show In Containers","Keeps buttons available around Auction House, Bazaar, Accessory Bag, Storage, and other container screens.",()->ConfigManager.get().inventoryButtons.showInContainers,v->ConfigManager.get().inventoryButtons.showInContainers=v))
            .add(SettingDefinition.toggle("general.inventoryButtons.potionEffects","Show Potion Effects","Shows the vanilla status-effect panel beside the player inventory.",()->ConfigManager.get().inventoryButtons.showPotionEffects,v->ConfigManager.get().inventoryButtons.showPotionEffects=v))
            .add(SettingDefinition.slider("general.inventoryButtons.scale","Button Scale","Changes inventory button size while keeping positions anchored to the GUI.",.75,1.25,()->ConfigManager.get().inventoryButtons.scale,v->ConfigManager.get().inventoryButtons.scale=v))
            .add(SettingDefinition.toggle("general.inventoryButtons.tooltips","Button Tooltips","Shows commands and click controls when a button is hovered.",()->ConfigManager.get().inventoryButtons.showTooltips,v->ConfigManager.get().inventoryButtons.showTooltips=v));
        items
            .add(SettingDefinition.toggle("general.itemRarity.enabled","Rarity Backgrounds","Shows the appropriate rarity-colored background behind supported SkyBlock items.",()->ConfigManager.get().itemRarity.enabled,v->ConfigManager.get().itemRarity.enabled=v))
            .add(SettingDefinition.toggle("general.itemRarity.dungeonInfo","Dungeon Drop Info","Shows a dungeon item's original floor and quality roll above its rarity line.",()->ConfigManager.get().itemRarity.showDungeonFloorAndQuality,v->ConfigManager.get().itemRarity.showDungeonFloorAndQuality=v));
        ConfigSubcategory chat=ui.add("chat_copy","Chat","Message copying and Bestiary chat preferences.")
            .add(SettingDefinition.toggle("general.chatCopy.enabled","Enable Chat Copy","Enables the Chat Copy keybind while the chat screen is open.",()->ConfigManager.get().chatCopy.enabled,v->ConfigManager.get().chatCopy.enabled=v))
            .add(SettingDefinition.chord("general.chatCopy.binding","Copy Keybind","Hover a chat message and press this keybind to copy its plain text.",()->ConfigManager.get().chatCopy.binding,v->ConfigManager.get().chatCopy.binding=v));

        ConfigCategory gameplay=register("gameplay","Gameplay","Combat readability and menu shortcuts.");
        gameplay.add("compact_damage","Compact Damage","Configure five-hit damage averaging and its world-space display.")
            .add(SettingDefinition.toggle("general.compactDamage","Compact Damage","Replaces rapid damage indicators with one moving five-hit average above the target.",()->ConfigManager.get().compactDamage,v->{ConfigManager.get().compactDamage=v;if(!v)name.skyveil.client.combat.CompactDamageManager.clear();}))
            .add(SettingDefinition.choice("general.compactDamageStyle","Display Style","Changes the appearance of the compact damage label.",List.of("Minimal","Neon","Crimson"),
                ()->switch(ConfigManager.get().compactDamageStyle){case "NEON"->"Neon";case "CRIMSON"->"Crimson";default->"Minimal";},
                value->ConfigManager.get().compactDamageStyle=value.toUpperCase(java.util.Locale.ROOT)))
            .add(SettingDefinition.toggle("general.compactDamage.crimsonSwipe","Crimson Swipe Damage","Adds secondary Crimson Armor Swipe damage to its triggering melee hit.",()->ConfigManager.get().compactDamageCrimsonSwipe,v->ConfigManager.get().compactDamageCrimsonSwipe=v))
            .add(SettingDefinition.toggle("general.compactDamage.ferocity","Ferocity Hits","Adds repeated Ferocity damage to its triggering melee hit.",()->ConfigManager.get().compactDamageFerocity,v->ConfigManager.get().compactDamageFerocity=v))
            .add(SettingDefinition.toggle("general.compactDamage.venomous","Venomous / Poison Damage","Adds green Venomous and poison ticks without consuming melee-hit slots.",()->ConfigManager.get().compactDamageVenomous,v->ConfigManager.get().compactDamageVenomous=v))
            .add(SettingDefinition.toggle("general.compactDamage.fire","Fire / Burning Damage","Adds orange Fire Aspect and burning ticks without consuming melee-hit slots.",()->ConfigManager.get().compactDamageFire,v->ConfigManager.get().compactDamageFire=v))
            .add(SettingDefinition.toggle("general.compactDamage.thunderlord","Thunderlord Damage","Adds blue Thunderlord and Thunderbolt damage to its triggering melee hit.",()->ConfigManager.get().compactDamageThunderlord,v->ConfigManager.get().compactDamageThunderlord=v))
            .add(SettingDefinition.toggle("general.compactDamage.pet","Pet Damage","Adds separately colored active-pet damage to the latest melee hit.",()->ConfigManager.get().compactDamagePet,v->ConfigManager.get().compactDamagePet=v))
            .add(SettingDefinition.toggle("general.compactDamage.other","Other Secondary Damage","Adds recognized uncategorized secondary damage without consuming hit slots.",()->ConfigManager.get().compactDamageOther,v->ConfigManager.get().compactDamageOther=v));
        items
            .add(SettingDefinition.toggle("general.itemProtection.enabled","Enable Item Protection","Prevents locked items from being moved or dropped.",()->ConfigManager.get().itemProtection.enabled,v->ConfigManager.get().itemProtection.enabled=v))
            .add(SettingDefinition.keybind("general.itemProtection.key","Lock / Link Key","Tap while hovering to lock or unlock. Hold and click slots to link them.",()->ConfigManager.get().itemProtection.lockKey,v->ConfigManager.get().itemProtection.lockKey=v))
            .add(SettingDefinition.toggle("general.itemProtection.icon","Show Lock Icon","Shows a lock mark over protected items.",()->ConfigManager.get().itemProtection.showLockIcon,v->ConfigManager.get().itemProtection.showLockIcon=v))
            .add(SettingDefinition.slider("general.itemProtection.opacity","Lock Icon Opacity","Changes the transparency of lock marks.",.25,1.0,()->ConfigManager.get().itemProtection.lockIconOpacity,v->ConfigManager.get().itemProtection.lockIconOpacity=v))
            .add(SettingDefinition.toggle("general.itemProtection.feedback","Blocked-action Feedback","Shows a short message when an item action is blocked.",()->ConfigManager.get().itemProtection.feedback,v->ConfigManager.get().itemProtection.feedback=v));
        gameplay.add("controls","Controls & Shortcuts","Manage Wardrobe number keys and custom command keybinds.")
            .add(SettingDefinition.toggle("general.wardrobe.numberKeys","Wardrobe Number Keys","Press number keys in the Wardrobe menu to equip the matching slot.",()->ConfigManager.get().wardrobe.numberKeys,v->ConfigManager.get().wardrobe.numberKeys=v))
            .add(SettingDefinition.toggle("general.customKeybinds.enabled","Custom Command Keybinds","Enables every saved custom command keybind without deleting them.",()->ConfigManager.get().customKeybinds.enabled,v->ConfigManager.get().customKeybinds.enabled=v))
            .add(SettingDefinition.button("general.customKeybinds.manage","Manage Keybinds","Add, edit, enable, disable, or delete custom command keybinds.",name.skyveil.client.customkeybind.CustomKeybindManagementScreen::open));
        chat
            .add(SettingDefinition.toggle("bestiary.hideRewards","Bestiary","Keeps Bestiary tier-up messages visible while hiding their reward section from chat.",()->ConfigManager.get().bestiary.hideRewards,v->ConfigManager.get().bestiary.hideRewards=v));

        ConfigCategory mining=register("mining","Mining","Mining progress and commission tracking.");
        mining.add("crystal_hollows_map","Crystal Hollows Map","A north-up region map with your live position.")
            .add(SettingDefinition.toggle("mining.crystalHollowsMap.enabled","Crystal Hollows Map","Shows your position and discovered entrances (D: Divan, G: Goblin Queen, J: Temple, P: City, B: Bal, K: King Yolkar). Entry points update on re-entry and reset between worlds. Move and resize in HUD Layout. Hides while holding TAB.",()->ConfigManager.get().crystalHollowsMap.enabled,v->ConfigManager.get().crystalHollowsMap.enabled=v));
        mining.add("corpse_waypoints","Corpse Waypoints","Locate loaded frozen corpses around Base Camp and in Glacite Mineshafts.")
            .add(SettingDefinition.toggle("mining.corpses.enabled","Corpse Waypoints","Colored corpse markers and distances visible through walls.",()->ConfigManager.get().corpseWaypoints.enabled,v->ConfigManager.get().corpseWaypoints.enabled=v))
            .add(SettingDefinition.toggle("mining.corpses.lapis","Lapis Corpses","Show blue Lapis corpse waypoints.",()->ConfigManager.get().corpseWaypoints.lapis,v->ConfigManager.get().corpseWaypoints.lapis=v))
            .add(SettingDefinition.toggle("mining.corpses.tungsten","Tungsten Corpses","Show grey Tungsten corpse waypoints.",()->ConfigManager.get().corpseWaypoints.tungsten,v->ConfigManager.get().corpseWaypoints.tungsten=v))
            .add(SettingDefinition.toggle("mining.corpses.umber","Umber Corpses","Show gold Umber corpse waypoints.",()->ConfigManager.get().corpseWaypoints.umber,v->ConfigManager.get().corpseWaypoints.umber=v));
        mining.add("pickaxe_ability","Pickaxe Ability","Live pickaxe ability cooldown from Hypixel widgets.")
            .add(SettingDefinition.toggle("mining.pickaxeAbility.enabled","Pickaxe Ability Cooldown","Shows the server's cooldown and ready status. Enable Pickaxe Ability in Hypixel /widgets. Move and resize in HUD Layout.",()->ConfigManager.get().pickaxeAbility.enabled,v->ConfigManager.get().pickaxeAbility.enabled=v));
        mining.add("commissions","Commissions","Live progress from the Hypixel Commissions widget.")
            .add(SettingDefinition.toggle("mining.commissions.enabled","Commissions Display","Shows commission progress and completion. Enable Commissions in Hypixel /widgets to supply live updates.",()->ConfigManager.get().commissions.enabled,v->ConfigManager.get().commissions.enabled=v));

        ConfigCategory hunting=register("hunting","Hunting","Shard collection, missing attributes, and Bazaar value tools.");
        hunting.add("hunting_box","Hunting Box","Display owned shard quantities and their current total value.")
            .add(SettingDefinition.toggle("hunting.huntingBoxValue","Hunting Box Value","Shows owned shard quantities and current Bazaar sell values beside the Hunting Box.",()->ConfigManager.get().hunting.huntingBoxValue,v->ConfigManager.get().hunting.huntingBoxValue=v));
        hunting.add("attribute_menu","Attribute Menu","Track collection progress, missing shards, and maxing costs.")
            .add(SettingDefinition.toggle("hunting.attributeProgress","Attribute Progress","Shows collection progress and missing Attribute Shards beside the Attribute Menu.",()->ConfigManager.get().hunting.attributeProgress,v->ConfigManager.get().hunting.attributeProgress=v))
            .add(SettingDefinition.toggle("hunting.attributePricing","Shard Prices","Adds current Bazaar acquisition prices and required-cost totals to the Attribute Menu panel.",()->ConfigManager.get().hunting.attributePricing,v->ConfigManager.get().hunting.attributePricing=v));

        ConfigCategory pets=register("pets","Pets","Pet HUD information and pet-related message controls.");
        pets.add("pet_display","Pet Display","Customize the equipped-pet HUD and its information.")
            .add(SettingDefinition.toggle("hud.petDisplay.enabled","Enable Pet Display","Shows the currently equipped SkyBlock pet and synchronized level data.",()->ConfigManager.get().petDisplay.enabled,v->ConfigManager.get().petDisplay.enabled=v))
            .add(SettingDefinition.choice("hud.petDisplay.style","Display Style","Changes the Pet Display panel and progress-bar presentation.",List.of("Panel","Minimal"),
                ()->"MINIMAL".equals(ConfigManager.get().petDisplay.style)?"Minimal":"Panel",
                value->ConfigManager.get().petDisplay.style=value.toUpperCase(java.util.Locale.ROOT)))
            .add(SettingDefinition.slider("hud.petDisplay.opacity","Background Opacity","Controls the panel opacity.",0,1,()->ConfigManager.get().petDisplay.backgroundOpacity,v->ConfigManager.get().petDisplay.backgroundOpacity=v))
            .add(SettingDefinition.slider("hud.petDisplay.scale","Display Scale","Uniformly scales the panel, text, icons, and progress bar.",.25,2,()->ConfigManager.get().petDisplay.scale,v->ConfigManager.get().petDisplay.scale=v))
            .add(SettingDefinition.toggle("hud.petDisplay.progress","XP Progress Bar","Shows progress through the pet's current level.",()->ConfigManager.get().petDisplay.showProgressBar,v->ConfigManager.get().petDisplay.showProgressBar=v))
            .add(SettingDefinition.toggle("hud.petDisplay.item","Attached Pet Item","Shows the item attached to the equipped pet.",()->ConfigManager.get().petDisplay.showPetItem,v->ConfigManager.get().petDisplay.showPetItem=v));
        pets.add("messages","Pet Messages","Choose which automatic pet notifications remain visible.")
            .add(SettingDefinition.toggle("pets.hideAutoPetRuleMessage","Hide Autopet Message","Hides only Hypixel's Autopet equipped-your-pet notification.",()->ConfigManager.get().petDisplay.hideAutoPetRuleMessage,v->ConfigManager.get().petDisplay.hideAutoPetRuleMessage=v));
        ConfigCategory slayers=register("slayers","Slayers","Boss-specific warnings and world highlights for Slayer encounters.");
        ConfigSubcategory voidgloom=slayers.add("voidgloom","Voidgloom","Visual assistance for Voidgloom Seraph mechanics.");
        voidgloom.add(SettingDefinition.toggle("slayers.voidgloom.lasers.enabled","Highlight Lasers","Draw visible lines over Broken Heart Radiation lasers.",()->ConfigManager.get().voidgloom.highlightLasers,v->ConfigManager.get().voidgloom.highlightLasers=v));
        voidgloom.add(SettingDefinition.color("slayers.voidgloom.lasers.color","Laser Color","Hex color for laser lines.",()->ConfigManager.get().voidgloom.laserColor,v->ConfigManager.get().voidgloom.laserColor=v));
        voidgloom.add(SettingDefinition.toggle("slayers.voidgloom.beacon.enabled","Highlight Beacon","Highlight Yang Glyphs and show their explosion timer.",()->ConfigManager.get().voidgloom.highlightBeacon,v->ConfigManager.get().voidgloom.highlightBeacon=v));
        voidgloom.add(SettingDefinition.color("slayers.voidgloom.beacon.color","Beacon Color","Hex color for Yang Glyph highlights.",()->ConfigManager.get().voidgloom.beaconColor,v->ConfigManager.get().voidgloom.beaconColor=v));
        voidgloom.add(SettingDefinition.toggle("slayers.voidgloom.heads.enabled","Highlight Heads","Highlight floating Nukekubi Fixation heads.",()->ConfigManager.get().voidgloom.highlightHeads,v->ConfigManager.get().voidgloom.highlightHeads=v));
        voidgloom.add(SettingDefinition.color("slayers.voidgloom.heads.color","Head Color","Hex color for Nukekubi head highlights.",()->ConfigManager.get().voidgloom.headColor,v->ConfigManager.get().voidgloom.headColor=v));
    }
}
