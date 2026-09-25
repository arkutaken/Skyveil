package name.skyveil.client.storage;

import com.mojang.serialization.DynamicOps;
import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.cache.SkyveilCacheManager;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.SkyveilTheme;
import name.skyveil.client.itemrarity.ItemRarityDetector;
import name.skyveil.client.itemrarity.SkyblockGuiItemDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persistent storage workspace shown beside Hypixel's live Storage containers. */
public final class StoragePreviewManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-storage-preview");
    private static final Pattern ENDER_CHEST=Pattern.compile("Ender Chest.*\\((\\d+)/\\d+\\)",Pattern.CASE_INSENSITIVE);
    private static final Pattern BACKPACK=Pattern.compile("Backpack.*\\(Slot #(\\d+)\\)",Pattern.CASE_INSENSITIVE);
    private static final int SCHEMA=1;
    private static final Palette DARK_PALETTE=new Palette(0x55000000,0xFF15171C,0xFF24272E,0xFF30343D,0xFFF4F2F7,0xFFAAAEB8,0xFF555A65,0xFF343842);
    private static final Palette DEFAULT_PALETTE=new Palette(0x44000000,0xFFC6C6C6,0xFF777777,0xFF999999,0xFF303030,0xFF555555,0xFF555555,0xFF707070);
    private static final Map<String,CachedStorage> CACHE=new LinkedHashMap<>();
    /** Cached stacks are immutable snapshots, so their preview rarity only needs parsing once. */
    private static final Map<ItemStack,Integer> PREVIEW_RARITY=new WeakHashMap<>();
    private static String loadedAccount;
    private static AbstractContainerScreen<?> observedScreen;
    private static int pendingFingerprint=Integer.MIN_VALUE,pendingConfirmations;
    private static boolean observedDirty,persistenceDirty;
    private static List<String> ownedSession=List.of();
    private static List<ClickArea> clickAreas=List.of();
    private static List<StorageSlotArea> storageSlotAreas=List.of();
    private static long lastPageSwitch;
    private static String cursorTargetKey;
    private static double cursorX,cursorY;
    private static int scroll,maxScroll;
    private static ControlBounds controls;
    private StoragePreviewManager(){}

    public static void tick(Minecraft client){ensureAccount(client);}

    /** Captures the active container and hands stable NBT to the unified shutdown cache. */
    public static void shutdown(Minecraft client){
        if(client==null||client.level==null||loadedAccount==null)return;
        if(client.screen instanceof AbstractContainerScreen<?> screen){String key=storageKeyFromTitle(screen.getTitle().getString());if(key!=null)capture(screen,key);}
        flushToCache(client);
    }
    public static void disconnect(Minecraft client){if(client!=null&&client.level!=null)flushToCache(client);loadedAccount=null;CACHE.clear();PREVIEW_RARITY.clear();observedScreen=null;ownedSession=List.of();clickAreas=List.of();storageSlotAreas=List.of();controls=null;observedDirty=false;persistenceDirty=false;}

    public static void observeAndRender(AbstractContainerScreen<?> screen,GuiGraphicsExtractor graphics,Slot hovered,int leftPos,int topPos,int imageWidth,int imageHeight,int mouseX,int mouseY){
        Minecraft client=Minecraft.getInstance();ensureAccount(client);if(client.player==null)return;String title=screen.getTitle().getString();String key=storageKeyFromTitle(title);if(!isStorageContext(title)){controls=null;return;}
        boolean enabled=ConfigManager.get().storagePreview;
        if(enabled){restoreCursor(client,key);if(key!=null)observeStable(screen,key);renderWorkspace(graphics,screen,key,leftPos,topPos,imageWidth,imageHeight,mouseX,mouseY);graphics.text(client.font,"Inventory",leftPos+8,topPos+imageHeight-94,palette().text(),false);}
        int controlX=enabled?leftPos+imageWidth-1:leftPos+imageWidth+inventoryButtonColumnWidth()+4;
        renderControls(graphics,screen,controlX,topPos+imageHeight-97,mouseX,mouseY);
    }

    public static void captureBeforeClose(AbstractContainerScreen<?> screen){
        if(screen==null||!ConfigManager.get().storagePreview)return;String key=storageKeyFromTitle(screen.getTitle().getString());if(key!=null)capture(screen,key);
        if(screen==observedScreen){observedScreen=null;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;}
    }

    // Require matching observations after a dirty packet before replacing a cached
    // page, avoiding an intermediate empty/partial container during page changes.
    private static void observeStable(AbstractContainerScreen<?> screen,String key){
        if(screen!=observedScreen){observedScreen=screen;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;observedDirty=true;}
        if(!observedDirty)return;
        int fingerprint=fingerprint(screen);if(pendingFingerprint==Integer.MIN_VALUE){pendingFingerprint=fingerprint;pendingConfirmations=1;return;}
        if(fingerprint!=pendingFingerprint){pendingFingerprint=fingerprint;pendingConfirmations=1;return;}
        if(++pendingConfirmations==2){capture(screen,key);observedDirty=false;}
    }

    /** Marks the active storage snapshot dirty only when vanilla receives authoritative slot data. */
    public static void onContainerUpdate(int containerId){
        Minecraft client=Minecraft.getInstance();
        if(!(client.screen instanceof AbstractContainerScreen<?> screen)||screen.getMenu().containerId!=containerId||storageKeyFromTitle(screen.getTitle().getString())==null)return;
        observedDirty=true;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;
    }

    private static void capture(AbstractContainerScreen<?> screen,String key){
        Minecraft client=Minecraft.getInstance();ensureAccount(client);if(loadedAccount==null)return;
        boolean ownershipChanged=rememberOwned(key);
        ArrayList<ItemStack> items=new ArrayList<>(screen.getMenu().slots.stream().filter(slot->client.player==null||slot.container!=client.player.getInventory()).sorted(Comparator.comparingInt(slot->slot.index)).map(slot->slot.getItem().copy()).toList());
        // The first row contains page controls, not stored items. Keeping it empty
        // prevents changing navigation decorations from rewriting an unchanged cache.
        for(int index=0;index<Math.min(9,items.size());index++)items.set(index,ItemStack.EMPTY);
        if(ownershipChanged)persistenceDirty=true;if(items.size()<=9)return;CachedStorage next=new CachedStorage(screen.getTitle().getString(),List.copyOf(items),fingerprint(items));CachedStorage previous=CACHE.get(key);if(equivalent(previous,next))return;CACHE.put(key,next);persistenceDirty=true;
    }

    private static void renderWorkspace(GuiGraphicsExtractor graphics,AbstractContainerScreen<?> screen,String activeKey,int leftPos,int topPos,int imageWidth,int imageHeight,int mouseX,int mouseY){
        Palette palette=palette();int screenWidth=graphics.guiWidth(),viewportBottom=Math.max(48,topPos+imageHeight-105);
        boolean overview=isStorageOverview(screen.getTitle().getString());if(overview){List<String> observedOwned=ownedKeys(screen);if(!observedOwned.isEmpty()&&!observedOwned.equals(ownedSession)){ownedSession=List.copyOf(observedOwned);persistenceDirty=true;}}
        ArrayList<String> keys=new ArrayList<>(ownedSession);if(keys.isEmpty())keys.addAll(CACHE.keySet());if(activeKey!=null&&!keys.contains(activeKey))keys.add(activeKey);keys.sort(Comparator.comparingInt(StoragePreviewManager::storageOrder));
        int itemColumns=9,cardWidth=176,columnCount=Math.max(1,(screenWidth-8)/(cardWidth+8)),gridWidth=columnCount*cardWidth+(columnCount-1)*8,startX=Math.max(4,(screenWidth-gridWidth)/2);ArrayList<Integer> allColumns=new ArrayList<>();for(int column=0;column<columnCount;column++)allColumns.add(startX+column*(cardWidth+8));
        ArrayList<StorageCard> cards=new ArrayList<>();LiveStorage live=activeKey==null?null:liveStorage(screen);for(String key:keys){boolean active=key.equals(activeKey);CachedStorage storage=CACHE.get(key);LiveStorage cardLive=active?live:null;int rows=cardLive!=null?Math.max(1,(cardLive.slots().size()+itemColumns-1)/itemColumns):storage==null?2:displayRows(storage,itemColumns);cards.add(new StorageCard(key,storage,cardLive,rows,25+rows*18,active));}
        ArrayList<PlacedCard> placed=new ArrayList<>();int cardIndex=0,y=8;while(cardIndex<cards.size()){int rowSize=Math.min(columnCount,cards.size()-cardIndex),rowHeight=0;for(int offset=0;offset<rowSize;offset++)rowHeight=Math.max(rowHeight,cards.get(cardIndex+offset).height());for(int offset=0;offset<rowSize;offset++)placed.add(new PlacedCard(cards.get(cardIndex+offset),allColumns.get(offset),y-scroll));cardIndex+=rowSize;y+=rowHeight+8;}
        ArrayList<ClickArea> clickable=new ArrayList<>();ArrayList<StorageSlotArea> interactiveSlots=new ArrayList<>();
        graphics.enableScissor(0,0,screenWidth-10,viewportBottom);try{for(PlacedCard placement:placed){StorageCard card=placement.card();int cardY=placement.y();if(cardY+card.height()<0||cardY>=viewportBottom)continue;drawStorageCard(graphics,card.key(),card.storage(),card.live(),placement.x(),cardY,card.rows(),itemColumns,cardWidth,card.active(),mouseX,mouseY,palette,interactiveSlots);int clickY=Math.max(0,cardY),clickBottom=Math.min(viewportBottom,cardY+card.height());if(clickBottom>clickY)clickable.add(new ClickArea(placement.x(),clickY,cardWidth,clickBottom-clickY,card.key()));}}finally{graphics.disableScissor();}interactiveSlots.removeIf(area->area.y()+area.height()<=0||area.y()>=viewportBottom);int contentBottom=y,maxContent=Math.max(0,contentBottom-8),viewportHeight=viewportBottom-8;maxScroll=Math.max(0,maxContent-viewportHeight);scroll=Math.max(0,Math.min(scroll,maxScroll));drawScrollbar(graphics,screenWidth,viewportBottom,palette);clickAreas=List.copyOf(clickable);storageSlotAreas=List.copyOf(interactiveSlots);
    }

    private static void drawStorageCard(GuiGraphicsExtractor graphics,String key,CachedStorage storage,LiveStorage live,int x,int y,int rows,int itemColumns,int cardWidth,boolean active,int mouseX,int mouseY,Palette palette,List<StorageSlotArea> interactiveSlots){
        int height=25+rows*18;graphics.fill(x,y,x+cardWidth,y+height,palette.background());graphics.outline(x,y,cardWidth,height,active?0xFFFFFFFF:palette.outline());if(active&&cardWidth>2&&height>2)graphics.outline(x+1,y+1,cardWidth-2,height-2,0xFFFFFFFF);graphics.text(Minecraft.getInstance().font,displayTitle(key),x+7,y+7,active?0xFFFFFFFF:palette.text(),false);
        if(live!=null){drawSlotGrid(graphics,x+7,y+20,itemColumns,rows,palette);for(int index=0;index<live.slots().size();index++){LiveSlot slot=live.slots().get(index);int slotX=x+7+(index%itemColumns)*18,slotY=y+20+(index/itemColumns)*18;drawItemSlot(graphics,slot.stack(),slotX,slotY,mouseX,mouseY,palette);interactiveSlots.add(new StorageSlotArea(slotX,slotY,18,18,slot.menuSlot()));}return;}
        if(storage==null){String first="Open storage to",second="display the preview";graphics.text(Minecraft.getInstance().font,first,x+(cardWidth-Minecraft.getInstance().font.width(first))/2,y+27,palette.secondary(),false);graphics.text(Minecraft.getInstance().font,second,x+(cardWidth-Minecraft.getInstance().font.width(second))/2,y+38,palette.secondary(),false);return;}
        drawSlotGrid(graphics,x+7,y+20,itemColumns,rows,palette);int content=Math.min(storage.items().size()-9,rows*itemColumns);for(int index=0;index<content;index++)drawItemSlot(graphics,storage.items().get(index+9),x+7+(index%itemColumns)*18,y+20+(index/itemColumns)*18,mouseX,mouseY,palette);
    }

    private static void drawSlotGrid(GuiGraphicsExtractor graphics,int x,int y,int columns,int rows,Palette palette){int width=columns*18,height=rows*18;graphics.fill(x,y,x+width,y+height,palette.slot());for(int column=0;column<=columns;column++)graphics.verticalLine(x+column*18,y,y+height,palette.slotOutline());for(int row=0;row<=rows;row++)graphics.horizontalLine(x,x+width,y+row*18,palette.slotOutline());}

    private static void drawScrollbar(GuiGraphicsExtractor graphics,int screenWidth,int viewportBottom,Palette palette){if(maxScroll<=0)return;int x=screenWidth-8,trackY=8,trackHeight=Math.max(20,viewportBottom-16),thumbHeight=Math.max(18,trackHeight*trackHeight/(trackHeight+maxScroll)),travel=trackHeight-thumbHeight,thumbY=trackY+(int)((long)scroll*travel/maxScroll);graphics.fill(x,trackY,x+4,trackY+trackHeight,palette.background());graphics.fill(x,thumbY,x+4,thumbY+thumbHeight,palette.outline());}

    private static void drawItemSlot(GuiGraphicsExtractor graphics,ItemStack stack,int x,int y,int mouseX,int mouseY,Palette palette){boolean hover=graphics.containsPointInScissor(mouseX,mouseY)&&inside(mouseX,mouseY,x,y,18,18);if(hover){graphics.fill(x,y,x+18,y+18,palette.hover());graphics.outline(x,y,18,18,SkyveilTheme.ACCENT);}if(!stack.isEmpty()){if(ConfigManager.get().itemRarity.enabled){int rarity=previewRarity(stack);if(rarity>=0)graphics.fill(x+1,y+1,x+17,y+17,(0x59<<24)|rarity);}graphics.item(stack,x+1,y+1);if(stack.getCount()>1||stack.isBarVisible())graphics.itemDecorations(Minecraft.getInstance().font,stack,x+1,y+1);if(hover)graphics.setTooltipForNextFrame(Minecraft.getInstance().font,stack,mouseX,mouseY);}}

    private static int previewRarity(ItemStack stack){Integer cached=PREVIEW_RARITY.get(stack);if(cached!=null)return cached;int rgb=-1;if(!SkyblockGuiItemDetector.isDecorative(stack)){ItemRarityDetector.Highlight highlight=ItemRarityDetector.detectHighlight(stack);if(highlight!=null)rgb=highlight.rgb();}PREVIEW_RARITY.put(stack,rgb);return rgb;}

    public static boolean isActive(AbstractContainerScreen<?> screen){return ConfigManager.get().storagePreview&&screen!=null&&(isStorageOverview(screen.getTitle().getString())||storageKeyFromTitle(screen.getTitle().getString())!=null);}
    /** null lets Minecraft's native container handler process live preview and inventory slots. */
    public static ClickResult mouseClicked(AbstractContainerScreen<?> screen,MouseButtonEvent event,boolean doubleClick){if(event.button()==0&&controls!=null&&controls.screen()==screen&&controls.contains(event.x(),event.y())){if(event.y()<controls.y()+24){ConfigManager.get().storagePreview=!ConfigManager.get().storagePreview;ConfigManager.save();screen.resize(screen.width,screen.height);}else{ConfigManager.get().storagePreviewTheme=nextTheme(ConfigManager.get().storagePreviewTheme);ConfigManager.save();}return ClickResult.CONSUME;}if(!isActive(screen)||!insideWorkspace(screen,event.x(),event.y()))return null;for(StorageSlotArea area:storageSlotAreas)if(area.contains(event.x(),event.y()))return null;if(event.button()!=0)return null;for(ClickArea area:clickAreas)if(area.contains(event.x(),event.y())){String active=storageKeyFromTitle(screen.getTitle().getString());if(area.key().equals(active))return ClickResult.CONSUME;if(!screen.getMenu().getCarried().isEmpty())return ClickResult.CONSUME;openPage(area.key());return ClickResult.CONSUME;}return null;}
    /** Overrides only live storage slots; player slots keep their normal Minecraft coordinates. */
    public static Boolean remappedHover(AbstractContainerScreen<?> screen,Slot slot,double mouseX,double mouseY){if(!isActive(screen))return null;Minecraft client=Minecraft.getInstance();if(client.player!=null&&slot.container==client.player.getInventory())return null;String active=storageKeyFromTitle(screen.getTitle().getString());if(active==null||!insideWorkspace(screen,mouseX,mouseY))return false;int menuSlot=screen.getMenu().slots.indexOf(slot);for(StorageSlotArea area:storageSlotAreas)if(area.menuSlot()==menuSlot)return area.contains(mouseX,mouseY);return false;}
    /** Compatible partial stacks in the active page, excluding Hypixel's first-row controls. */
    public static List<Integer> stackMergeTargets(AbstractContainerScreen<?> screen,Slot source){
        Minecraft client=Minecraft.getInstance();if(!isActive(screen)||storageKeyFromTitle(screen.getTitle().getString())==null||client.player==null||source==null||source.container!=client.player.getInventory()||source.getItem().isEmpty()||!source.getItem().isStackable()||!screen.getMenu().getCarried().isEmpty())return List.of();
        ItemStack moving=source.getItem();ArrayList<Integer> result=new ArrayList<>();for(int menuSlot=0;menuSlot<screen.getMenu().slots.size();menuSlot++){Slot target=screen.getMenu().slots.get(menuSlot);ItemStack existing=target.getItem();if(target.container==client.player.getInventory()||target.index<9||existing.isEmpty()||!target.mayPlace(moving)||!ItemStack.isSameItemSameComponents(moving,existing)||existing.getCount()>=target.getMaxStackSize(existing))continue;result.add(menuSlot);}return List.copyOf(result);
    }
    public static boolean isOverLiveStorage(AbstractContainerScreen<?> screen,double mouseX,double mouseY){if(!isActive(screen)||storageKeyFromTitle(screen.getTitle().getString())==null||!insideWorkspace(screen,mouseX,mouseY))return false;for(StorageSlotArea area:storageSlotAreas)if(area.contains(mouseX,mouseY))return true;return false;}
    public static boolean mouseScrolled(AbstractContainerScreen<?> screen,double mouseX,double mouseY,double vertical){if(!isActive(screen)||vertical==0||maxScroll==0)return false;if(vertical>0)scroll=Math.max(0,scroll-36);else scroll=Math.min(maxScroll,scroll+36);return true;}

    private static Palette palette(){return switch(ConfigManager.get().storagePreviewTheme){
        case "DARK"->DARK_PALETTE;
        default->DEFAULT_PALETTE;
    };}

    private static void ensureAccount(Minecraft client){
        if(client==null||client.player==null||client.level==null||!SkyveilCacheManager.isLoaded())return;String account=client.player.getUUID().toString();if(account.equals(loadedAccount))return;if(loadedAccount!=null&&persistenceDirty)flushToCache(client);loadedAccount=account;CACHE.clear();PREVIEW_RARITY.clear();observedScreen=null;observedDirty=false;persistenceDirty=false;ownedSession=List.of();clickAreas=List.of();storageSlotAreas=List.of();controls=null;scroll=0;maxScroll=0;lastPageSwitch=0;cursorTargetKey=null;pendingFingerprint=Integer.MIN_VALUE;pendingConfirmations=0;load(client);
    }

    private static void load(Minecraft client){CompoundTag root=SkyveilCacheManager.profileSection(loadedAccount,"storage");if(root==null||root.getIntOr("schema",0)!=SCHEMA)return;try{ArrayList<String> owned=new ArrayList<>();ListTag savedOwned=root.getListOrEmpty("owned");for(int index=0;index<Math.min(savedOwned.size(),32);index++){String key=savedOwned.getCompoundOrEmpty(index).getStringOr("key","");if((key.startsWith("ender:")||key.startsWith("backpack:"))&&!owned.contains(key))owned.add(key);}owned.sort(Comparator.comparingInt(StoragePreviewManager::storageOrder));ownedSession=List.copyOf(owned);DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());ListTag entries=root.getListOrEmpty("entries");for(int entryIndex=0;entryIndex<Math.min(entries.size(),32);entryIndex++){CompoundTag entry=entries.getCompoundOrEmpty(entryIndex);String key=entry.getStringOr("key","");int size=entry.getIntOr("size",0);if(key.isBlank()||size<=9||size>180)continue;ArrayList<ItemStack> items=new ArrayList<>(java.util.Collections.nCopies(size,ItemStack.EMPTY));ListTag savedItems=entry.getListOrEmpty("items");for(int itemIndex=0;itemIndex<Math.min(savedItems.size(),180);itemIndex++){CompoundTag saved=savedItems.getCompoundOrEmpty(itemIndex);int slot=saved.getIntOr("slot",-1);Tag encoded=saved.get("stack");if(slot<0||slot>=size||encoded==null)continue;try{items.set(slot,ItemStack.CODEC.parse(ops,encoded).result().orElse(ItemStack.EMPTY));}catch(Exception exception){LOGGER.debug("Skipped unreadable cached item in {} slot {}",key,slot,exception);}}CACHE.put(key,new CachedStorage(entry.getStringOr("title",key),List.copyOf(items),fingerprint(items)));if(!ownedSession.contains(key))rememberOwned(key);}persistenceDirty=false;}catch(Exception exception){LOGGER.warn("Could not restore Storage Preview from the unified cache",exception);}}

    private static void flushToCache(Minecraft client){
        if(!persistenceDirty||loadedAccount==null||client==null||client.level==null)return;try{DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());CompoundTag root=new CompoundTag();root.putInt("schema",SCHEMA);ListTag owned=new ListTag();for(String key:ownedSession){CompoundTag status=new CompoundTag();status.putString("key",key);status.putBoolean("opened",CACHE.containsKey(key));owned.add(status);}root.put("owned",owned);ListTag entries=new ListTag();for(var cacheEntry:CACHE.entrySet()){CachedStorage storage=cacheEntry.getValue();CompoundTag entry=new CompoundTag();entry.putString("key",cacheEntry.getKey());entry.putString("title",storage.title());entry.putInt("size",storage.items().size());ListTag items=new ListTag();for(int slot=0;slot<storage.items().size();slot++){ItemStack stack=storage.items().get(slot);if(stack.isEmpty())continue;Tag encoded;try{encoded=ItemStack.CODEC.encodeStart(ops,stack).result().orElse(null);}catch(Exception exception){LOGGER.debug("Skipped unwritable cached item in {} slot {}",cacheEntry.getKey(),slot,exception);continue;}if(encoded==null)continue;CompoundTag saved=new CompoundTag();saved.putInt("slot",slot);saved.put("stack",encoded);items.add(saved);}entry.put("items",items);entries.add(entry);}root.put("entries",entries);SkyveilCacheManager.putProfileSection(loadedAccount,"storage",root);persistenceDirty=false;}catch(Exception exception){LOGGER.error("Could not prepare Storage Preview cache state",exception);}
    }
    private static boolean equivalent(CachedStorage left,CachedStorage right){if(left==null||left.fingerprint()!=right.fingerprint()||left.items().size()!=right.items().size())return false;for(int index=0;index<left.items().size();index++){ItemStack a=left.items().get(index),b=right.items().get(index);if(a.getCount()!=b.getCount()||!ItemStack.isSameItemSameComponents(a,b))return false;}return true;}
    private static int fingerprint(AbstractContainerScreen<?> screen){Minecraft client=Minecraft.getInstance();ArrayList<ItemStack> items=new ArrayList<>();for(Slot slot:screen.getMenu().slots)if(client.player==null||slot.container!=client.player.getInventory())items.add(slot.getItem());return fingerprint(items);}
    private static int fingerprint(List<ItemStack> items){int value=items.size();for(ItemStack stack:items){value=31*value+stack.getCount();value=31*value+ItemStack.hashItemAndComponents(stack);}return value;}
    static boolean isStorageOverview(String title){return title!=null&&title.trim().equalsIgnoreCase("Storage");}
    private static boolean isStorageContext(String title){return isStorageOverview(title)||storageKeyFromTitle(title)!=null;}
    static String storageKeyFromTitle(String title){String value=title==null?"":title.trim();Matcher ender=ENDER_CHEST.matcher(value);if(ender.find())return "ender:"+ender.group(1);Matcher backpack=BACKPACK.matcher(value);return backpack.find()?"backpack:"+backpack.group(1):null;}
    static String storageKeyForOverviewSlot(int slot){if(slot>=9&&slot<18)return "ender:"+(slot-8);if(slot>=27&&slot<45)return "backpack:"+(slot-26);return null;}
    static int slotForStorageKey(String key){if(key==null)return -1;try{if(key.startsWith("ender:"))return 8+Integer.parseInt(key.substring(6));if(key.startsWith("backpack:"))return 26+Integer.parseInt(key.substring(9));}catch(NumberFormatException ignored){}return -1;}
    private static int storageOrder(String key){if(key==null)return Integer.MAX_VALUE;try{if(key.startsWith("ender:"))return Integer.parseInt(key.substring(6));if(key.startsWith("backpack:"))return 100+Integer.parseInt(key.substring(9));}catch(NumberFormatException ignored){}return Integer.MAX_VALUE;}
    private static boolean rememberOwned(String key){if(key==null||ownedSession.contains(key))return false;ArrayList<String> owned=new ArrayList<>(ownedSession);owned.add(key);owned.sort(Comparator.comparingInt(StoragePreviewManager::storageOrder));ownedSession=List.copyOf(owned);return true;}
    private static List<String> ownedKeys(AbstractContainerScreen<?> screen){ArrayList<String> result=new ArrayList<>();for(int slot=9;slot<18;slot++)if(ownedOverviewSlot(screen,slot,"red_stained_glass_pane"))result.add("ender:"+(slot-8));for(int slot=27;slot<45;slot++)if(ownedOverviewSlot(screen,slot,"brown_stained_glass_pane"))result.add("backpack:"+(slot-26));return result;}
    private static boolean ownedOverviewSlot(AbstractContainerScreen<?> screen,int slot,String unavailableItem){if(slot<0||slot>=screen.getMenu().slots.size())return false;ItemStack stack=screen.getMenu().slots.get(slot).getItem();if(stack.isEmpty())return false;var key=BuiltInRegistries.ITEM.getKey(stack.getItem());return key!=null&&!key.getPath().equals(unavailableItem);}
    private static LiveStorage liveStorage(AbstractContainerScreen<?> screen){Minecraft client=Minecraft.getInstance();ArrayList<LiveSlot> result=new ArrayList<>();for(int menuSlot=0;menuSlot<screen.getMenu().slots.size();menuSlot++){Slot slot=screen.getMenu().slots.get(menuSlot);if(client.player==null||slot.container!=client.player.getInventory())result.add(new LiveSlot(menuSlot,slot.index,slot.getItem()));}result.sort(Comparator.comparingInt(LiveSlot::containerSlot));if(result.size()<=9)return new LiveStorage(List.of());return new LiveStorage(List.copyOf(result.subList(9,result.size())));}
    private static void openPage(String key){Minecraft client=Minecraft.getInstance();if(client.getConnection()==null||key==null||System.currentTimeMillis()-lastPageSwitch<150)return;String command=null;if(key.startsWith("ender:"))command="enderchest "+key.substring(6);else if(key.startsWith("backpack:"))command="backpack "+key.substring(9);if(command==null)return;cursorX=client.mouseHandler.xpos();cursorY=client.mouseHandler.ypos();cursorTargetKey=key;lastPageSwitch=System.currentTimeMillis();client.getConnection().sendCommand(command);}
    private static void restoreCursor(Minecraft client,String activeKey){if(cursorTargetKey==null||!cursorTargetKey.equals(activeKey))return;InputConstants.grabOrReleaseMouse(client.getWindow(),InputConstants.CURSOR_NORMAL,cursorX,cursorY);client.mouseHandler.setIgnoreFirstMove();cursorTargetKey=null;}
    private static void renderControls(GuiGraphicsExtractor graphics,AbstractContainerScreen<?> screen,int x,int y,int mouseX,int mouseY){Palette palette=palette();int width=25,height=49;controls=new ControlBounds(screen,x,y,width,height);graphics.fill(x,y,x+width,y+height,palette.background());graphics.outline(x,y,width,height,palette.outline());boolean toggleHover=inside(mouseX,mouseY,x+2,y+2,21,21),themeHover=inside(mouseX,mouseY,x+2,y+26,21,21),enabled=ConfigManager.get().storagePreview;graphics.fill(x+2,y+2,x+23,y+23,toggleHover?palette.hover():(enabled?0xFF285C35:0xFF6B2929));graphics.outline(x+2,y+2,21,21,toggleHover?SkyveilTheme.ACCENT:palette.slotOutline());String toggle=enabled?"ON":"OFF";graphics.text(Minecraft.getInstance().font,toggle,x+(width-Minecraft.getInstance().font.width(toggle))/2,y+8,0xFFFFFFFF,false);int themeColor=switch(ConfigManager.get().storagePreviewTheme){case "DARK"->0xFF15171C;default->0xFFC6C6C6;};graphics.fill(x+2,y+26,x+23,y+47,themeHover?palette.hover():themeColor);graphics.outline(x+2,y+26,21,21,themeHover?SkyveilTheme.ACCENT:palette.slotOutline());String letter=switch(ConfigManager.get().storagePreviewTheme){case "DARK"->"D";default->"L";};graphics.text(Minecraft.getInstance().font,letter,x+(width-Minecraft.getInstance().font.width(letter))/2,y+32,"DEFAULT".equals(ConfigManager.get().storagePreviewTheme)?0xFF303030:0xFFFFFFFF,false);if(toggleHover)graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,List.of(Component.literal("Storage Preview: "+(enabled?"Enabled":"Disabled")),Component.literal("Click to "+(enabled?"disable":"enable"))),mouseX,mouseY);else if(themeHover)graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font,List.of(Component.literal("Storage color: "+themeLabel(ConfigManager.get().storagePreviewTheme)),Component.literal("Click to cycle colors")),mouseX,mouseY);}
    private static int inventoryButtonColumnWidth(){return Math.max(15,(int)Math.round(20*ConfigManager.get().inventoryButtons.scale));}
    private static String nextTheme(String current){return switch(current){case "DEFAULT"->"DARK";case "DARK"->"DEFAULT";default->"DEFAULT";};}
    private static String themeLabel(String theme){return switch(theme){case "DARK"->"Dark";default->"Default";};}
    private static int displayRows(CachedStorage storage,int columns){int last=-1;for(int index=9;index<storage.items().size();index++)if(!storage.items().get(index).isEmpty())last=index-9;return Math.max(1,last/columns+1);}
    private static String displayTitle(String key){if(key.startsWith("ender:"))return "Ender Chest Page "+key.substring(6);if(key.startsWith("backpack:"))return "Backpack Slot "+key.substring(9);return key;}
    static boolean withinViewport(double x,double y,int screenWidth,int viewportBottom){
        return x>=0&&x<screenWidth-10&&y>=0&&y<viewportBottom;
    }
    private static boolean insideWorkspace(AbstractContainerScreen<?> screen,double x,double y){
        var bounds=(name.skyveil.client.mixin.ContainerScreenAccessor)screen;
        return withinViewport(x,y,screen.width,Math.max(48,bounds.skyveil$getTopPos()+bounds.skyveil$getImageHeight()-105));
    }
    private static boolean inside(double px,double py,int x,int y,int width,int height){return px>=x&&px<x+width&&py>=y&&py<y+height;}
    private record CachedStorage(String title,List<ItemStack> items,int fingerprint){}
    private record ClickArea(int x,int y,int width,int height,String key){boolean contains(double px,double py){return inside(px,py,x,y,width,height);}}
    private record StorageSlotArea(int x,int y,int width,int height,int menuSlot){boolean contains(double px,double py){return inside(px,py,x,y,width,height);}}
    private record LiveSlot(int menuSlot,int containerSlot,ItemStack stack){}
    private record LiveStorage(List<LiveSlot> slots){}
    private record StorageCard(String key,CachedStorage storage,LiveStorage live,int rows,int height,boolean active){}
    private record PlacedCard(StorageCard card,int x,int y){}
    private record ControlBounds(AbstractContainerScreen<?> screen,int x,int y,int width,int height){boolean contains(double px,double py){return inside(px,py,x,y,width,height);}}
    public record ClickResult(int slot,int button,ContainerInput input){private static final ClickResult CONSUME=new ClickResult(-1,0,ContainerInput.PICKUP);}
    private record Palette(int canvas,int background,int slot,int hover,int text,int secondary,int outline,int slotOutline){}
}
