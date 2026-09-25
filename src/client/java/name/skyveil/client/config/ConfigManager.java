package name.skyveil.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Loads, migrates, and validates user preferences. The mutable model is used by
 * client UI code; save snapshots it before queuing serialized disk writes.
 * Runtime observations belong in SkyveilCacheManager, not in this settings file.
 */
public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("skyveil-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"Skyveil-Config");thread.setDaemon(true);return thread;});
    private static SkyveilConfig config = new SkyveilConfig();

    private ConfigManager() {}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("skyveil.json");}

    public static SkyveilConfig get() { return config; }

    public static void load() {
        Path path=path();
        if (!Files.exists(path)) { save(); return; }
        boolean rewrite = false;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement tree=JsonParser.parseReader(reader);
            rewrite=migrateLegacyTree(tree);
            SkyveilConfig loaded = GSON.fromJson(tree, SkyveilConfig.class);
            if (loaded != null) config = loaded;
            else { config = new SkyveilConfig(); rewrite = true; }
        } catch (Exception exception) {
            LOGGER.warn("Could not read {}; defaults will be used", path, exception);
            try { Files.copy(path, path.resolveSibling("skyveil.json.corrupt"), StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException backupFailure) { LOGGER.warn("Could not preserve malformed config {}", path, backupFailure); }
            config = new SkyveilConfig();
            rewrite = true;
        }
        rewrite |= needsMigration();
        validate();
        rewrite |= migrate();
        if (rewrite) { LOGGER.info("Migrated or repaired {} using safe defaults", path); save(); }
    }

    private static boolean needsMigration() {
        return config.darkMode==null || config.storagePreviewTheme==null || "DARKISH_PURPLE".equals(config.darkMode) || "Darkish Purple".equals(config.darkMode)
            || "DARK_PURPLE".equals(config.darkMode) || "DARK_PURPLE".equals(config.storagePreviewTheme)
            || config.inventoryPreview!=null&&!"DARK".equals(config.inventoryPreview.backgroundColor)
            || config.zoom == null || config.bestiary == null || config.hunting == null
            || config.itemProtection == null || config.itemProtection.slotLinks == null || config.itemRarity == null || config.inventoryButtons == null
            || config.customKeybinds == null || config.chatCopy == null || config.wardrobe == null || config.inventoryPreview == null || config.skillXp == null || config.commissions == null || config.crystalHollowsMap == null || config.corpseWaypoints == null || config.pickaxeAbility == null || config.playerStats == null || config.performance == null || config.petDisplay == null || config.voidgloom == null || config.lockedInventorySlots == null
            || config.inventoryButtons.buttons == null || config.customKeybinds.bindings == null || config.chatCopy.binding == null || config.petDisplay.style == null;
    }

    private static void validate() {
        // Retired purple choices retain dark-mode intent without keeping a purple palette.
        if("DARKISH_PURPLE".equals(config.darkMode)||"Darkish Purple".equals(config.darkMode)||"DARK_PURPLE".equals(config.darkMode))config.darkMode="DARK";
        if(!"DEFAULT".equals(config.darkMode)&&!"DARK".equals(config.darkMode))config.darkMode="DEFAULT";
        if(!"DEFAULT".equals(config.storagePreviewTheme)&&!"DARK".equals(config.storagePreviewTheme) )config.storagePreviewTheme="DARK";
        if(!"NEON".equals(config.compactDamageStyle)&&!"CRIMSON".equals(config.compactDamageStyle)&&!"MINIMAL".equals(config.compactDamageStyle))config.compactDamageStyle="MINIMAL";
        if (config.zoom == null) config.zoom = new SkyveilConfig.Zoom();
        if (config.bestiary == null) config.bestiary = new SkyveilConfig.Bestiary();
        if (config.hunting == null) config.hunting = new SkyveilConfig.Hunting();
        config.hunting.attributeSort=validShardSort(config.hunting.attributeSort);
        if (config.itemProtection == null) config.itemProtection = new SkyveilConfig.ItemProtection();
        if(config.itemProtection.protectedItems==null)config.itemProtection.protectedItems=new java.util.HashSet<>();
        if (config.itemProtection.slotLinks == null) config.itemProtection.slotLinks = new java.util.LinkedHashMap<>();
        if (config.itemRarity == null) config.itemRarity = new SkyveilConfig.ItemRarity();
        if (config.inventoryButtons == null) config.inventoryButtons = new SkyveilConfig.InventoryButtons();
        if (config.inventoryButtons.buttons == null) config.inventoryButtons.buttons = new java.util.ArrayList<>();
        if (config.customKeybinds == null) config.customKeybinds = new SkyveilConfig.CustomKeybinds();
        if (config.customKeybinds.bindings == null) config.customKeybinds.bindings = new java.util.ArrayList<>();
        if (config.chatCopy == null) config.chatCopy = new SkyveilConfig.ChatCopy();
        config.chatCopy.binding=name.skyveil.client.chatcopy.ChatCopyManager.normalize(config.chatCopy.binding);
        if (config.wardrobe == null) config.wardrobe = new SkyveilConfig.Wardrobe();
        if(config.inventoryPreview==null)config.inventoryPreview=new SkyveilConfig.InventoryPreview();
        config.inventoryPreview.backgroundOpacity=Double.isFinite(config.inventoryPreview.backgroundOpacity)?Math.max(0,Math.min(1,config.inventoryPreview.backgroundOpacity)):.4;
        config.inventoryPreview.backgroundColor="DARK";
        config.inventoryPreview.hudX=Math.max(-1,config.inventoryPreview.hudX);config.inventoryPreview.hudY=Math.max(-1,config.inventoryPreview.hudY);
        config.inventoryPreview.scale=Double.isFinite(config.inventoryPreview.scale)?Math.max(.25,Math.min(2,config.inventoryPreview.scale)):1;
        if(config.crystalHollowsMap==null)config.crystalHollowsMap=new SkyveilConfig.CrystalHollowsMap();
        config.crystalHollowsMap.hudX=Math.max(0,config.crystalHollowsMap.hudX);
        config.crystalHollowsMap.hudY=Math.max(0,config.crystalHollowsMap.hudY);
        config.crystalHollowsMap.scale=Double.isFinite(config.crystalHollowsMap.scale)?Math.clamp(config.crystalHollowsMap.scale,.25,2):1;
        if(config.corpseWaypoints==null)config.corpseWaypoints=new SkyveilConfig.CorpseWaypoints();
        if(config.pickaxeAbility==null)config.pickaxeAbility=new SkyveilConfig.PickaxeAbility();
        config.pickaxeAbility.hudX=Math.max(0,config.pickaxeAbility.hudX);config.pickaxeAbility.hudY=Math.max(0,config.pickaxeAbility.hudY);
        config.pickaxeAbility.scale=Double.isFinite(config.pickaxeAbility.scale)?Math.clamp(config.pickaxeAbility.scale,.25,2):1;
        if(config.commissions==null)config.commissions=new SkyveilConfig.Commissions();
        config.commissions.hudX=Math.max(0,config.commissions.hudX);config.commissions.hudY=Math.max(0,config.commissions.hudY);
        config.commissions.scale=Double.isFinite(config.commissions.scale)?Math.clamp(config.commissions.scale,.25,2):1;
        if(config.skillXp==null)config.skillXp=new SkyveilConfig.SkillXp();
        config.skillXp.hudX=Math.max(-1,config.skillXp.hudX);config.skillXp.hudY=Math.max(-1,config.skillXp.hudY);
        config.skillXp.scale=Double.isFinite(config.skillXp.scale)?Math.max(.25,Math.min(2,config.skillXp.scale)):1;
        if(config.playerStats==null)config.playerStats=new SkyveilConfig.PlayerStats();
        if(config.playerStats.positions==null)config.playerStats.positions=new java.util.EnumMap<>(name.skyveil.client.stats.PlayerStat.class);
        for(var stat:name.skyveil.client.stats.PlayerStat.values()){
            var position=config.playerStats.positions.computeIfAbsent(stat,key->new SkyveilConfig.StatPosition());
            position.hudX=Math.max(-1,position.hudX);position.hudY=Math.max(-1,position.hudY);
            position.scale=Double.isFinite(position.scale)?Math.max(.25,Math.min(2,position.scale)):1;
        }
        if (config.performance == null) config.performance = new SkyveilConfig.Performance();
        config.performance.hudX=Math.max(0,config.performance.hudX);
        config.performance.hudY=Math.max(0,config.performance.hudY);
        config.performance.scale=Double.isFinite(config.performance.scale)?Math.max(.25,Math.min(2,config.performance.scale)):1;
        if (config.petDisplay == null) config.petDisplay = new SkyveilConfig.PetDisplay();
        if (config.voidgloom == null) config.voidgloom = new SkyveilConfig.Voidgloom();
        config.voidgloom.laserColor=opaque(config.voidgloom.laserColor);
        config.voidgloom.beaconColor=opaque(config.voidgloom.beaconColor);
        config.voidgloom.headColor=opaque(config.voidgloom.headColor);
        if(!"PANEL".equals(config.petDisplay.style)&&!"MINIMAL".equals(config.petDisplay.style))config.petDisplay.style="PANEL";
        if (config.lockedInventorySlots == null) config.lockedInventorySlots = new java.util.HashSet<>();
        config.lockedInventorySlots.removeIf(slot -> slot == null || slot < 0 || slot > 40);
        java.util.LinkedHashMap<Integer,java.util.List<Integer>> sanitizedLinks=new java.util.LinkedHashMap<>();
        java.util.HashSet<Integer> linkedInventory=new java.util.HashSet<>();
        for(var entry:config.itemProtection.slotLinks.entrySet()){
            if(entry.getKey()==null||entry.getKey()<0||entry.getKey()>8||entry.getValue()==null)continue;
            java.util.ArrayList<Integer> destinations=new java.util.ArrayList<>();
            for(Integer destination:entry.getValue())if(destination!=null&&destination>=9&&destination<=35&&linkedInventory.add(destination))destinations.add(destination);
            if(!destinations.isEmpty())sanitizedLinks.put(entry.getKey(),destinations);
        }
        config.itemProtection.slotLinks=sanitizedLinks;
        config.itemProtection.lockIconOpacity=Math.max(.25,Math.min(1.0,config.itemProtection.lockIconOpacity));
        config.inventoryButtons.scale=Math.max(.75,Math.min(1.25,config.inventoryButtons.scale));
        config.petDisplay.hudX=Math.max(0,config.petDisplay.hudX);
        config.petDisplay.hudY=Math.max(0,config.petDisplay.hudY);
        config.petDisplay.scale=Double.isFinite(config.petDisplay.scale)?Math.max(.25,Math.min(2.0,config.petDisplay.scale)):1;
        config.petDisplay.backgroundOpacity=Math.max(0,Math.min(1.0,config.petDisplay.backgroundOpacity));
    }

    private static boolean migrate() {
        boolean changed = false;
        if (config.version < 2) {
            // Version 2 makes the inventory effect panel opt-in. Apply this once to existing
            // configs so their formerly default true value does not keep the panel visible.
            config.inventoryButtons.showPotionEffects = false;
            config.version = 2;
            changed = true;
        }
        if(config.version<15){config.version=15;changed=true;}
        if(config.version<16){config.hunting.huntingBoxValue=true;config.hunting.attributePricing=true;config.version=16;changed=true;}
        if(config.version<17){config.version=17;changed=true;}
        if(config.version<18){config.storagePreview=true;config.version=18;changed=true;}
        if(config.version<19){config.storagePreviewTheme=config.darkMode;config.version=19;changed=true;}
        if(config.version<20){config.inventoryButtons.showInContainers=true;config.version=20;changed=true;}
        if(config.version<21){config.version=21;changed=true;}
        if(config.version<22){config.compactDamageStyle="CLASSIC";config.version=22;changed=true;}
        if(config.version<23){if("CLASSIC".equals(config.compactDamageStyle))config.compactDamageStyle="MINIMAL";config.petDisplay.style="PANEL";config.version=23;changed=true;}
        if(config.version<24){if("NEON".equals(config.petDisplay.style))config.petDisplay.style="COSMIC";else if("GLASS".equals(config.petDisplay.style))config.petDisplay.style="RUNIC";config.version=24;changed=true;}
        if(config.version<25){if(!"MINIMAL".equals(config.petDisplay.style))config.petDisplay.style="PANEL";config.version=25;changed=true;}
        if(config.version<26){config.version=26;changed=true;}
        if(config.version<27){config.version=27;changed=true;}
        if(config.version<28){config.version=28;changed=true;}
        if(config.version<29){if("VOID".equals(config.compactDamageStyle))config.compactDamageStyle="MINIMAL";config.version=29;changed=true;}
        if(config.version<30){config.version=30;changed=true;}
        if(config.version<31){config.version=31;changed=true;}
        return changed;
    }

    private static int opaque(int color){return 0xFF000000|(color&0xFFFFFF);}

    private static String validShardSort(String value){
        return "RARITY".equals(value)||"QUANTITY".equals(value)||"PRICE".equals(value)?value:"RARITY";
    }

    /** Keep legacy shape conversion separate from model validation and disk writes. */
    static boolean migrateLegacyTree(JsonElement tree){return ConfigMigration.migrate(tree);}

    // Capture serialized settings before scheduling I/O; the worker must not read
    // the mutable model while the user is still editing controls.
    public static void save() {
        validate();
        String json=GSON.toJson(config);
        IO.execute(()->write(json));
    }

    /** Flushes queued settings at normal client shutdown so the daemon writer cannot lose the last edit. */
    public static void shutdown() {
        IO.shutdown();
        try {
            if (!IO.awaitTermination(5, TimeUnit.SECONDS)) LOGGER.warn("Timed out while flushing {}", path());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while flushing {}", path());
        }
    }

    /** Serial disk writes run off the client/render thread; callers only snapshot the small model. */
    private static void write(String json) {
        Path path=path();
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling("skyveil.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) { writer.write(json); }
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException unsupported) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) { LOGGER.error("Could not save {}", path, exception); }
    }
}
