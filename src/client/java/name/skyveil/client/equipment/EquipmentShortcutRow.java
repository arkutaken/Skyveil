package name.skyveil.client.equipment;

import com.mojang.serialization.DynamicOps;
import name.skyveil.client.cache.SkyveilCacheManager;
import name.skyveil.client.gui.ContainerDarkModeRenderer;
import name.skyveil.client.hunting.AttributeShardResolver;
import name.skyveil.client.inventorybuttons.InventoryButtonManager;
import name.skyveil.client.itemrarity.ItemRarityRenderer;
import name.skyveil.client.itemsearch.SkyBlockEquipmentCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Four-slot inventory shortcut backed by real stacks observed in Hypixel's Equipment menu. */
public final class EquipmentShortcutRow {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-equipment-showcase");
    private static final int CACHE_SCHEMA=1;
    private static final String[] LABELS={"Necklace","Cloak","Belt","Gloves / Bracelet"};
    private static final ItemStack[] EQUIPPED={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
    private static Object connection;
    private static String loadedAccount;
    private static boolean dirty;
    private static int observedContainer=-1;
    private static boolean observationDirty=true;
    private EquipmentShortcutRow(){}

    public static void observe(AbstractContainerScreen<?> screen){
        if(screen==null||!matchesMenuTitle(screen.getTitle().getString()))return;int container=screen.getMenu().containerId;if(container!=observedContainer){observedContainer=container;observationDirty=true;}if(!observationDirty)return;observationDirty=false;Minecraft client=Minecraft.getInstance();if(client.player==null||!client.player.getUUID().toString().equals(loadedAccount))return;int before=fingerprint();String title=normalizeTitle(screen.getTitle().getString());
        if(title.contains("equipment sets")){Integer selected=selectedEquipmentSetColumn(screen);if(selected!=null&&observeColumn(screen,selected,true)){saveIfChanged(client,before);return;}}
        else if((title.contains("loadouts")||title.equals("your equipment and stats"))&&observeColumn(screen,1,true)){saveIfChanged(client,before);return;}
        ItemStack[] found={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};int[] scores={Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE};
        for(var slot:screen.getMenu().slots){ItemStack stack=slot.getItem();if(stack.isEmpty()||client.player!=null&&slot.container==client.player.getInventory())continue;var lore=stack.get(net.minecraft.core.component.DataComponents.LORE);List<Component> lines=lore==null?List.of():lore.lines();String name=stack.getHoverName().getString();int type=classify(stack,name,lines);if(type<0||isEmptyControl(name))continue;String text=(name+" "+lines.stream().map(Component::getString).reduce("",(left,right)->left+" "+right)).toLowerCase(Locale.ROOT);int score=(text.contains("currently equipped")||text.contains("click to unequip")||text.contains("active loadout")||text.contains("selected loadout")?10_000:0)-slot.index;if(score>scores[type]){scores[type]=score;found[type]=stack.copy();}}
        // Keep the last real stack when the wardrobe briefly swaps its contents during page updates.
        for(int index=0;index<EQUIPPED.length;index++)if(!found[index].isEmpty())EQUIPPED[index]=found[index];
        saveIfChanged(client,before);
    }

    // The server marks the selected loadout with a lime-dye control. Use its
    // column rather than treating every equipment set as currently equipped.
    private static Integer selectedEquipmentSetColumn(AbstractContainerScreen<?> screen){
        for(Slot slot:screen.getMenu().slots)if(slot.index>35&&slot.index<45&&itemPath(slot.getItem()).equals("lime_dye"))return slot.index%9;
        return null;
    }

    private static boolean observeColumn(AbstractContainerScreen<?> screen,int column,boolean replaceEmpty){
        Minecraft client=Minecraft.getInstance();List<Slot> columnSlots=screen.getMenu().slots.stream()
            .filter(slot->slot.index%9==column)
            .filter(slot->client.player==null||slot.container!=client.player.getInventory())
            .filter(slot->!itemPath(slot.getItem()).equals("black_stained_glass_pane"))
            .sorted(Comparator.comparingInt(slot->slot.index)).toList();
        if(columnSlots.size()<4)return false;
        for(int index=0;index<4;index++){ItemStack stack=columnSlots.get(index).getItem();boolean empty=stack.isEmpty()||isPlaceholderName(stack.getHoverName().getString());if(!empty||replaceEmpty)EQUIPPED[index]=empty?ItemStack.EMPTY:stack.copy();}
        return true;
    }

    public static void tick(Minecraft client){refreshAccount(client);}
    public static void onContainerUpdate(int containerId){if(containerId==observedContainer)observationDirty=true;}
    public static void screenClosed(AbstractContainerScreen<?> screen){if(screen!=null&&screen.getMenu().containerId==observedContainer){observedContainer=-1;observationDirty=true;}}
    public static void shutdown(Minecraft client){flushToCache(client);}
    public static void disconnect(Minecraft client){flushToCache(client);connection=null;loadedAccount=null;dirty=false;observedContainer=-1;observationDirty=true;clear();}

    public static void render(GuiGraphicsExtractor graphics,int guiLeft,int guiTop,int mouseX,int mouseY){Minecraft client=Minecraft.getInstance();int x=guiLeft+77,y=guiTop+8;var palette=ContainerDarkModeRenderer.controlPalette();
        // Vanilla's offhand artwork extends outside its logical 18x18 slot. Cover the
        // complete column first so its frame cannot show around the equipment slots.
        graphics.fill(x-1,y-1,x+19,y+73,palette.panel());
        for(int index=0;index<4;index++){int slotY=y+index*18,background=inside(mouseX,mouseY,x,slotY,18,18)?palette.hover():palette.slot();graphics.fill(x,slotY,x+18,slotY+18,background);graphics.outline(x,slotY,18,18,inside(mouseX,mouseY,x,slotY,18,18)?palette.accent():palette.outline());ItemStack stack=EQUIPPED[index];if(!stack.isEmpty()){ItemRarityRenderer.drawBelowItem(graphics,stack,x+1,slotY+1);graphics.item(stack,x+1,slotY+1);}if(inside(mouseX,mouseY,x,slotY,18,18)){if(!stack.isEmpty())graphics.setTooltipForNextFrame(client.font,stack,mouseX,mouseY);else graphics.setComponentTooltipForNextFrame(client.font,List.of(Component.literal(LABELS[index]),Component.literal("Click to open Equipment")),mouseX,mouseY);}}}

    public static boolean mouseClicked(int guiLeft,int guiTop,double mouseX,double mouseY,int button){if(button!=0||!inside(mouseX,mouseY,guiLeft+77,guiTop+8,18,72))return false;InventoryButtonManager.executeCommand("equipment");return true;}
    static boolean matchesMenuTitle(String title){String normalized=normalizeTitle(title);return normalized.contains("equipment")||normalized.contains("loadout");}
    static int classify(ItemStack stack,String name,List<Component> lines){int type=classifyLore(lines);if(type>=0)return type;type=SkyBlockEquipmentCatalog.typeForInternalId(AttributeShardResolver.resolveInternalId(stack));return type>=0?type:SkyBlockEquipmentCatalog.typeFromText(name);}
    static int classifyLore(List<Component> lines){for(int index=lines.size()-1;index>=0;index--){String line=lines.get(index).getString().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]+"," ").trim();if(word(line,"NECKLACE"))return 0;if(word(line,"CLOAK"))return 1;if(word(line,"BELT"))return 2;if(word(line,"GLOVES")||word(line,"BRACELET"))return 3;}return -1;}
    public static boolean isOffhandSlot(Slot slot){Minecraft client=Minecraft.getInstance();return slot!=null&&client.player!=null&&slot.container==client.player.getInventory()&&slot.getContainerSlot()==40;}
    private static boolean isEmptyControl(String name){return isPlaceholderName(name);}
    static boolean isPlaceholderName(String name){String normalized=name==null?"":name.trim().toLowerCase(Locale.ROOT);return normalized.startsWith("empty")||normalized.startsWith("slot ");}
    private static String normalizeTitle(String title){return title==null?"":title.trim().toLowerCase(Locale.ROOT);}
    private static String itemPath(ItemStack stack){if(stack==null||stack.isEmpty())return "";var key=BuiltInRegistries.ITEM.getKey(stack.getItem());return key==null?"":key.getPath();}
    private static boolean word(String line,String value){return (" "+line+" ").contains(" "+value+" ");}
    private static void refreshAccount(Minecraft client){
        if(client==null||!SkyveilCacheManager.isLoaded())return;Object current=client.getConnection();if(connection!=current){if(loadedAccount!=null&&dirty)flushToCache(client);connection=current;loadedAccount=null;clear();}
        if(client.player==null||client.level==null)return;String account=client.player.getUUID().toString();if(account.equals(loadedAccount))return;if(loadedAccount!=null&&dirty)flushToCache(client);
        loadedAccount=account;clear();load(client);dirty=false;
    }
    private static void load(Minecraft client){CompoundTag root=SkyveilCacheManager.profileSection(loadedAccount,"equipment");if(root==null||root.getIntOr("schema",0)!=CACHE_SCHEMA)return;try{DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());ListTag items=root.getListOrEmpty("items");for(int index=0;index<Math.min(items.size(),EQUIPPED.length);index++){CompoundTag saved=items.getCompoundOrEmpty(index);int slot=saved.getIntOr("slot",-1);if(slot<0||slot>=EQUIPPED.length||saved.get("stack")==null)continue;EQUIPPED[slot]=ItemStack.CODEC.parse(ops,saved.get("stack")).result().orElse(ItemStack.EMPTY);}}catch(Exception exception){LOGGER.warn("Could not restore equipment showcase",exception);}}
    private static void saveIfChanged(Minecraft client,int before){if(before!=fingerprint()&&loadedAccount!=null)dirty=true;}
    private static void flushToCache(Minecraft client){if(!dirty||loadedAccount==null||client==null||client.level==null)return;try{DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());CompoundTag root=new CompoundTag();root.putInt("schema",CACHE_SCHEMA);ListTag items=new ListTag();for(int slot=0;slot<EQUIPPED.length;slot++){ItemStack stack=EQUIPPED[slot];if(stack.isEmpty())continue;CompoundTag saved=new CompoundTag();saved.putInt("slot",slot);saved.put("stack",ItemStack.CODEC.encodeStart(ops,stack).getOrThrow());items.add(saved);}root.put("items",items);SkyveilCacheManager.putProfileSection(loadedAccount,"equipment",root);dirty=false;}catch(Exception exception){LOGGER.warn("Could not prepare equipment showcase cache",exception);}}
    private static int fingerprint(){int value=1;for(ItemStack stack:EQUIPPED){value=31*value+stack.getCount();value=31*value+ItemStack.hashItemAndComponents(stack);}return value;}
    private static void clear(){for(int index=0;index<EQUIPPED.length;index++)EQUIPPED[index]=ItemStack.EMPTY;}
    private static boolean inside(double mouseX,double mouseY,int x,int y,int width,int height){return mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+height;}
}
