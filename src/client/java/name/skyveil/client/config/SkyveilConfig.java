package name.skyveil.client.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent client-only settings. Public fields keep the JSON easy to inspect and migrate. */
public final class SkyveilConfig {
    // Configuration schema version, not the mod's release version. Migrations use
    // this to repair older JSON while field initializers supply new defaults.
    public int version = 31;
    public boolean auctionTooltip=true;
    public boolean fullCraftCost=true;
    public boolean bazaarTooltip=true;
    public boolean blackConfigTheme = false;
    public String darkMode = "DEFAULT";
    public boolean showSwitchedItemName = true;
    public boolean hudAlignmentSnap = true;
    public boolean scrollableTooltips = false;
    public boolean compactDamage = false;
    public String compactDamageStyle = "MINIMAL";
    public boolean compactDamageCrimsonSwipe = false;
    public boolean compactDamageFerocity = false;
    public boolean compactDamageVenomous = false;
    public boolean compactDamageFire = false;
    public boolean compactDamageThunderlord = false;
    public boolean compactDamagePet = false;
    public boolean compactDamageOther = false;
    public boolean storagePreview = true;
    public String storagePreviewTheme = "DARK";
    public Zoom zoom = new Zoom();
    public Bestiary bestiary = new Bestiary();
    public Hunting hunting = new Hunting();
    public ItemProtection itemProtection = new ItemProtection();
    public ItemRarity itemRarity = new ItemRarity();
    public InventoryButtons inventoryButtons = new InventoryButtons();
    public CustomKeybinds customKeybinds = new CustomKeybinds();
    public ChatCopy chatCopy = new ChatCopy();
    public Wardrobe wardrobe = new Wardrobe();
    public InventoryPreview inventoryPreview = new InventoryPreview();
    public SkillXp skillXp = new SkillXp();
    public Commissions commissions = new Commissions();
    public CrystalHollowsMap crystalHollowsMap=new CrystalHollowsMap();
    public static final class CrystalHollowsMap {
        public boolean enabled=true;
        public int hudX=240,hudY=8;
        public double scale=1;
    }
    public PickaxeAbility pickaxeAbility = new PickaxeAbility();
    public CorpseWaypoints corpseWaypoints = new CorpseWaypoints();
    public static final class CorpseWaypoints {
        public boolean enabled=true, lapis=true, tungsten=true, umber=true;
    }
    public PlayerStats playerStats = new PlayerStats();
    public Performance performance = new Performance();
    public PetDisplay petDisplay = new PetDisplay();
    public Voidgloom voidgloom = new Voidgloom();
    public java.util.Set<Integer> lockedInventorySlots = new java.util.HashSet<>();
    public static final class Zoom {
        public boolean enabled=true;
        public int key=org.lwjgl.glfw.GLFW.GLFW_KEY_C;
    }
    public static final class Bestiary {
        public boolean hideRewards=false;
    }
    public static final class Hunting {
        public boolean huntingBoxValue=true;
        public boolean attributeProgress=true;
        public boolean attributePricing=true;
        public String attributeSort="RARITY";
        public boolean attributeSortDescending=true;
    }
    public static final class ItemProtection {
        public boolean protectItems=true;
        public int protectItemKey=org.lwjgl.glfw.GLFW.GLFW_KEY_P;
        public java.util.Set<String> protectedItems=new java.util.HashSet<>();
        public boolean enabled=true;
        public int lockKey=org.lwjgl.glfw.GLFW.GLFW_KEY_L;
        public boolean showLockIcon=true;
        public double lockIconOpacity=.65;
        public boolean feedback=true;
        public Map<Integer,List<Integer>> slotLinks=new LinkedHashMap<>();
    }
    public static final class ItemRarity {
        public boolean enabled=true;
        public boolean showDungeonFloorAndQuality=true;
    }
    public static final class InventoryButtons {
        public boolean enabled=true;
        public boolean showPotionEffects=false;
        public boolean showTooltips=true;
        public boolean showInContainers=true;
        public double scale=1.0;
        public java.util.List<name.skyveil.client.inventorybuttons.InventoryButtonDefinition> buttons=new java.util.ArrayList<>();
    }
    public static final class CustomKeybinds {
        public boolean enabled=true;
        public java.util.List<name.skyveil.client.customkeybind.CustomKeybindDefinition> bindings=new java.util.ArrayList<>();
    }
    public static final class ChatCopy {
        public boolean enabled=true;
        public name.skyveil.client.chatcopy.ChatCopyBinding binding=new name.skyveil.client.chatcopy.ChatCopyBinding();
    }
    public static final class Wardrobe {
        public boolean numberKeys=true;
    }
    public static final class StatPosition {
        public int hudX=-1,hudY=-1;
        public double scale=1;
    }
    public static final class PlayerStats {
        public boolean enabled=true;
        public java.util.Map<name.skyveil.client.stats.PlayerStat,StatPosition> positions=new java.util.EnumMap<>(name.skyveil.client.stats.PlayerStat.class);
        public PlayerStats(){for(var stat:name.skyveil.client.stats.PlayerStat.values())positions.put(stat,new StatPosition());}
    }
    public static final class InventoryPreview {
        public double backgroundOpacity=.4;
        public String backgroundColor="DARK";
        public boolean enabled=true;
        public int hudX=-1,hudY=-1;
        public double scale=1;
    }
    public static final class PickaxeAbility {
        public boolean enabled=true;
        public int hudX=8,hudY=65;
        public double scale=1;
    }
    public static final class Commissions {
        public boolean enabled=true;
        public int hudX=8,hudY=100;
        public double scale=1;
    }
    public static final class SkillXp {
        public boolean enabled=true;
        public int hudX=-1,hudY=-1;
        public double scale=1;
    }
    public static final class Performance {
        public boolean enabled=true;
        public int hudX=8,hudY=8;
        public double scale=1.0;
    }
    public static final class PetDisplay {
        public boolean enabled=true;
        public String style="PANEL";
        public int hudX=8,hudY=225;
        public double scale=1.0;
        public double backgroundOpacity=.60;
        public boolean showProgressBar=true;
        public boolean showPetItem=true;
        public boolean hideAutoPetRuleMessage=false;
    }
    public static final class Voidgloom {
        public boolean highlightLasers=true;
        public int laserColor=0xFF55FFFF;
        public boolean highlightBeacon=true;
        public int beaconColor=0xFFFF5555;
        public boolean highlightHeads=true;
        public int headColor=0xFFFFAA00;
    }
}
