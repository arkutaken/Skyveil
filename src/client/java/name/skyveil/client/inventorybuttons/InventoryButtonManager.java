package name.skyveil.client.inventorybuttons;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.itemsearch.SkyBlockHeadIcons;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.ref.WeakReference;
import java.util.*;

/** In-memory access, validation, persistence, icon resolution, and one-click command dispatch. */
public final class InventoryButtonManager {
    private static List<InventoryButtonDefinition> cachedSource;
    private static List<InventoryButtonDefinition> cachedButtons=List.of();
    private static Map<InventoryButtonPosition,InventoryButtonDefinition> cachedLayout=Map.of();
    private static final Map<String,ItemStack> ICONS=new HashMap<>();
    private static WeakReference<net.minecraft.client.gui.screens.inventory.InventoryScreen> managementInventory=new WeakReference<>(null);
    private InventoryButtonManager() {}
    /**
     * Validates and indexes persisted button definitions during client bootstrap.
     * ItemStack construction must remain lazy: in a packaged launch the client
     * entrypoint runs before item components have been bound.
     */
    public static void initialize(){refreshCache();}
    public static List<InventoryButtonDefinition> all(){refreshCache();return cachedButtons;}
    public static Map<InventoryButtonPosition,InventoryButtonDefinition> layout(){refreshCache();return cachedLayout;}
    public static InventoryButtonDefinition at(InventoryButtonPosition position){return layout().get(position);}
    public static void save(InventoryButtonDefinition definition){
        sanitize();InventoryButtonDefinition normalized=normalize(definition.copy());
        ConfigManager.get().inventoryButtons.buttons.removeIf(button->Objects.equals(button.id,normalized.id)||InventoryButtonPosition.parse(button.position)==InventoryButtonPosition.parse(normalized.position));
        ConfigManager.get().inventoryButtons.buttons.add(normalized);invalidate();ConfigManager.save();
    }
    public static void delete(String id){ConfigManager.get().inventoryButtons.buttons.removeIf(button->Objects.equals(button.id,id));invalidate();ConfigManager.save();}
    public static ItemStack icon(String id){
        if(SkyBlockHeadIcons.isKey(id)){String cacheKey=id;return ICONS.computeIfAbsent(cacheKey,ignored->{ItemStack head=SkyBlockHeadIcons.resolve(cacheKey);return head.isEmpty()?new ItemStack(Items.CHEST):head;});}
        Identifier key=parseId(id);String cacheKey=key.toString();return ICONS.computeIfAbsent(cacheKey,ignored->BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElseGet(()->new ItemStack(Items.CHEST)));
    }
    public static void beginManagement(net.minecraft.client.gui.screens.inventory.InventoryScreen screen){managementInventory=new WeakReference<>(screen);}
    public static boolean isManagement(net.minecraft.client.gui.screens.Screen screen){return screen==managementInventory.get();}
    public static String normalizeCommand(String command){String value=command==null?"":command.replace('\r',' ').replace('\n',' ').trim();while(value.startsWith("/"))value=value.substring(1).trim();return value;}
    public static void execute(InventoryButtonDefinition definition){
        if(definition==null||!definition.enabled||!ConfigManager.get().inventoryButtons.enabled)return;
        executeCommand(definition.command);
    }
    public static void executeCommand(String raw){
        String command=normalizeCommand(raw);Minecraft client=Minecraft.getInstance();if(command.isBlank()||client.getConnection()==null)return;
        if(!ClientCommandInternals.executeCommand(command))client.getConnection().sendCommand(command);
    }
    private static InventoryButtonDefinition normalize(InventoryButtonDefinition button){
        if(button.id==null||button.id.isBlank())button.id=UUID.randomUUID().toString();
        button.command=normalizeCommand(button.command);
        if(SkyBlockHeadIcons.isKey(button.icon))button.icon=SkyBlockHeadIcons.exists(button.icon)?button.icon:"minecraft:chest";
        else{Identifier icon=parseId(button.icon);button.icon=BuiltInRegistries.ITEM.containsKey(icon)&&BuiltInRegistries.ITEM.getValue(icon)!=Items.AIR?icon.toString():"minecraft:chest";}
        button.position=InventoryButtonPosition.parse(button.position).name();return button;
    }
    private static void sanitize(){
        var config=ConfigManager.get().inventoryButtons;if(config.buttons==null)config.buttons=new ArrayList<>();
        Set<String> ids=new HashSet<>(),positions=new HashSet<>();config.buttons.removeIf(button->{if(button==null)return true;normalize(button);return button.command.isBlank()||!ids.add(button.id)||!positions.add(button.position);});
    }
    private static void refreshCache(){
        List<InventoryButtonDefinition> source=ConfigManager.get().inventoryButtons.buttons;
        if(source==cachedSource)return;
        sanitize();source=ConfigManager.get().inventoryButtons.buttons;
        cachedSource=source;cachedButtons=List.copyOf(source);
        EnumMap<InventoryButtonPosition,InventoryButtonDefinition> layout=new EnumMap<>(InventoryButtonPosition.class);
        for(InventoryButtonDefinition button:source)if(button.enabled)layout.put(InventoryButtonPosition.parse(button.position),button);
        cachedLayout=Collections.unmodifiableMap(layout);
    }
    private static void invalidate(){cachedSource=null;}
    private static Identifier parseId(String raw){String value=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);int split=value.indexOf(':');if(split<0)return Identifier.fromNamespaceAndPath("minecraft",value.isBlank()?"chest":value);if(split==0||split==value.length()-1)return Identifier.fromNamespaceAndPath("minecraft","chest");try{return Identifier.fromNamespaceAndPath(value.substring(0,split),value.substring(split+1));}catch(Exception ignored){return Identifier.fromNamespaceAndPath("minecraft","chest");}}
}
