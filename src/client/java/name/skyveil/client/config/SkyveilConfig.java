package name.skyveil.client.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent client-only settings. Public fields keep the JSON easy to inspect and migrate. */
public final class SkyveilConfig {
    public int version = 20;
    public String darkMode = "DEFAULT";
    public boolean scrollableTooltips = false;
    public boolean compactDamage = false;
    public boolean storagePreview = true;
    public String storagePreviewTheme = "DARK_PURPLE";
    public Zoom zoom = new Zoom();
    public Bestiary bestiary = new Bestiary();
    public Hunting hunting = new Hunting();
    public ItemProtection itemProtection = new ItemProtection();
    public ItemRarity itemRarity = new ItemRarity();
    public InventoryButtons inventoryButtons = new InventoryButtons();
    public CustomKeybinds customKeybinds = new CustomKeybinds();
    public Wardrobe wardrobe = new Wardrobe();
    public PetDisplay petDisplay = new PetDisplay();
    public MapSettings map = new MapSettings();
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
        public boolean enabled=true;
        public int lockKey=org.lwjgl.glfw.GLFW.GLFW_KEY_L;
        public boolean showLockIcon=true;
        public double lockIconOpacity=.65;
        public boolean feedback=true;
        public Map<Integer,List<Integer>> slotLinks=new LinkedHashMap<>();
    }
    public static final class ItemRarity {
        public boolean enabled=true;
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
    public static final class Wardrobe {
        public boolean numberKeys=true;
    }
    public static final class PetDisplay {
        public boolean enabled=true;
        public int hudX=8,hudY=225;
        public double scale=1.0;
        public double backgroundOpacity=.60;
        public boolean showProgressBar=true;
        public boolean showPetItem=true;
        public boolean hideAutoPetRuleMessage=false;
    }
    public static final class MapSettings {
        public boolean enabled=true;
        public int largeMapKey=org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK;
        public boolean minimapEnabled=true;
        public int minimapX=-1,minimapY=8;
        public double minimapSize=128.0,minimapScale=1.0,minimapZoom=2.5,minimapOpacity=.72;
        public boolean minimapDirection=true,minimapNpcs=true,minimapCustom=true;
        public double largeMapScale=.90;
        public boolean largePlayer=true,largeCoordinates=true,largeNpcs=true,largeZones=true,largeCustom=true;
        public boolean npcsEnabled=true,npcTooltips=true,npcTooltipCoordinates=true;
        public double npcMarkerSize=12.0,playerMarkerSize=12.0;
        public boolean debug=false;
    }
}
