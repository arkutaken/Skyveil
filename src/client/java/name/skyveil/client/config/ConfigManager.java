package name.skyveil.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            || config.zoom == null || config.bestiary == null || config.hunting == null
            || config.itemProtection == null || config.itemProtection.slotLinks == null || config.itemRarity == null || config.inventoryButtons == null
            || config.customKeybinds == null || config.wardrobe == null || config.petDisplay == null || config.map == null || config.lockedInventorySlots == null
            || config.inventoryButtons.buttons == null || config.customKeybinds.bindings == null;
    }

    private static void validate() {
        if("DARKISH_PURPLE".equals(config.darkMode)||"Darkish Purple".equals(config.darkMode))config.darkMode="DARK_PURPLE";
        if(!"DEFAULT".equals(config.darkMode)&&!"DARK".equals(config.darkMode)&&!"DARK_PURPLE".equals(config.darkMode))config.darkMode="DEFAULT";
        if(!"DEFAULT".equals(config.storagePreviewTheme)&&!"DARK".equals(config.storagePreviewTheme)&&!"DARK_PURPLE".equals(config.storagePreviewTheme))config.storagePreviewTheme="DARK_PURPLE";
        if (config.zoom == null) config.zoom = new SkyveilConfig.Zoom();
        if (config.bestiary == null) config.bestiary = new SkyveilConfig.Bestiary();
        if (config.hunting == null) config.hunting = new SkyveilConfig.Hunting();
        config.hunting.attributeSort=validShardSort(config.hunting.attributeSort);
        if (config.itemProtection == null) config.itemProtection = new SkyveilConfig.ItemProtection();
        if (config.itemProtection.slotLinks == null) config.itemProtection.slotLinks = new java.util.LinkedHashMap<>();
        if (config.itemRarity == null) config.itemRarity = new SkyveilConfig.ItemRarity();
        if (config.inventoryButtons == null) config.inventoryButtons = new SkyveilConfig.InventoryButtons();
        if (config.inventoryButtons.buttons == null) config.inventoryButtons.buttons = new java.util.ArrayList<>();
        if (config.customKeybinds == null) config.customKeybinds = new SkyveilConfig.CustomKeybinds();
        if (config.customKeybinds.bindings == null) config.customKeybinds.bindings = new java.util.ArrayList<>();
        if (config.wardrobe == null) config.wardrobe = new SkyveilConfig.Wardrobe();
        if (config.petDisplay == null) config.petDisplay = new SkyveilConfig.PetDisplay();
        if (config.map == null) config.map = new SkyveilConfig.MapSettings();
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
        config.petDisplay.scale=Math.max(.5,Math.min(2.0,config.petDisplay.scale));
        config.petDisplay.backgroundOpacity=Math.max(0,Math.min(1.0,config.petDisplay.backgroundOpacity));
        config.map.minimapX=Math.max(-1,config.map.minimapX);
        config.map.minimapY=Math.max(0,config.map.minimapY);
        config.map.minimapSize=Math.max(80.0,Math.min(220.0,config.map.minimapSize));
        config.map.minimapScale=Math.max(.5,Math.min(2.0,config.map.minimapScale));
        config.map.minimapZoom=Math.max(1.0,Math.min(8.0,config.map.minimapZoom));
        config.map.minimapOpacity=Math.max(.20,Math.min(1.0,config.map.minimapOpacity));
        config.map.largeMapScale=Math.max(.50,Math.min(1.0,config.map.largeMapScale));
        config.map.npcMarkerSize=Math.max(8.0,Math.min(20.0,config.map.npcMarkerSize));
        config.map.playerMarkerSize=Math.max(8.0,Math.min(20.0,config.map.playerMarkerSize));
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
        return changed;
    }

    private static String validShardSort(String value){
        return "RARITY".equals(value)||"QUANTITY".equals(value)||"PRICE".equals(value)?value:"RARITY";
    }

    /** Converts legacy JSON shapes before Gson binds them to the current strongly typed model. */
    static boolean migrateLegacyTree(JsonElement tree){
        if(tree==null||!tree.isJsonObject())return false;
        boolean changed=false;JsonObject root=tree.getAsJsonObject();
        for(String key:new String[]{"trophyFishing","bobberTimer","baitSack","fishingNavigation","seaCreatures","itemPrices","trophyDiamondCaught","trophyTierCounts","trophyTotalCounts"})
            if(root.remove(key)!=null)changed=true;
        JsonElement protectionElement=root.get("itemProtection");
        if(protectionElement!=null&&protectionElement.isJsonObject()){
            JsonElement linksElement=protectionElement.getAsJsonObject().get("slotLinks");
            if(linksElement!=null&&linksElement.isJsonObject())for(var entry:linksElement.getAsJsonObject().entrySet())
                if(entry.getValue()!=null&&entry.getValue().isJsonPrimitive()){
                    com.google.gson.JsonArray destinations=new com.google.gson.JsonArray();destinations.add(entry.getValue());entry.setValue(destinations);changed=true;
                }
        }
        JsonElement rarityElement=root.get("itemRarity");
        if(rarityElement!=null&&rarityElement.isJsonObject()){
            JsonObject rarity=rarityElement.getAsJsonObject();
            String[] legacyFlags={"common","uncommon","rare","epic","legendary","mythic","divine","special","verySpecial","supreme","ultimate","admin"};
            if(!rarity.has("enabled")){
                boolean enabled=false,found=false;
                for(String flag:legacyFlags)if(rarity.has(flag)&&rarity.get(flag).isJsonPrimitive()){
                    found=true;try{enabled|=rarity.get(flag).getAsBoolean();}catch(Exception ignored){}
                }
                rarity.addProperty("enabled",!found||enabled);changed=true;
            }
            String[] dead={"style","opacity","outlineOpacity","outlineThickness","common","uncommon","rare","epic","legendary","mythic","divine","special","verySpecial","supreme","ultimate","admin"};
            for(String key:dead)if(rarity.remove(key)!=null)changed=true;
        }
        return changed;
    }

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
