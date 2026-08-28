package name.skyveil.client.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent client-only settings. Public fields keep the JSON easy to inspect and migrate. */
public final class SkyveilConfig {
    public int version = 30;
    public String darkMode = "DEFAULT";
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
    public String storagePreviewTheme = "DARK_PURPLE";
    public Zoom zoom = new Zoom();
    public Bestiary bestiary = new Bestiary();
    public Hunting hunting = new Hunting();
    public ItemProtection itemProtection = new ItemProtection();
    public ItemRarity itemRarity = new ItemRarity();
    public InventoryButtons inventoryButtons = new InventoryButtons();
    public CustomKeybinds customKeybinds = new CustomKeybinds();
    public ChatCopy chatCopy = new ChatCopy();
    public Wardrobe wardrobe = new Wardrobe();
    public PetDisplay petDisplay = new PetDisplay();
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
    public static final class ChatCopy {
        public boolean enabled=true;
        public name.skyveil.client.chatcopy.ChatCopyBinding binding=new name.skyveil.client.chatcopy.ChatCopyBinding();
    }
    public static final class Wardrobe {
        public boolean numberKeys=true;
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
}
