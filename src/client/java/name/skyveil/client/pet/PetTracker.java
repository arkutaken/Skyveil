package name.skyveil.client.pet;

import com.mojang.serialization.DynamicOps;
import name.skyveil.client.cache.SkyveilCacheManager;
import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.itemrarity.ItemRarityDetector;
import name.skyveil.client.itemrarity.SkyblockRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks one equipped-pet state with live TAB/widget authority and Pets-menu detail enrichment. */
public final class PetTracker {
    private static final Logger LOGGER=LoggerFactory.getLogger("Skyveil PetDebug");
    private static final Pattern SUMMON=Pattern.compile("(?i)\\byou\\s+summoned\\s+your\\s+(?:\\[\\s*lvl\\s*(\\d{1,4})\\s*]\\s*)?([^!\\r\\n]+?)(?:!|$)" );
    private static final Pattern AUTOPET=Pattern.compile("(?i)\\bauto\\s*pet\\s+equipped\\s+your\\s+(?:\\[\\s*lvl\\s*(\\d{1,4})\\s*]\\s*)?([^!\\r\\n]+?)(?:!|$)" );
    private static final Pattern TYPE=Pattern.compile("(?i)(?:\\\\?\")type(?:\\\\?\")\\s*:\\s*(?:\\\\?\")([a-z0-9_]+)");
    private static final Pattern EXP=Pattern.compile("(?i)(?:\\\\?\")exp(?:\\\\?\")\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern HELD_ITEM=Pattern.compile("(?i)(?:\\\\?\")heldItem(?:\\\\?\")\\s*:\\s*(?:\\\\?\")([a-z0-9_]+)");
    private static final Pattern TIER=Pattern.compile("(?i)(?:\\\\?\")tier(?:\\\\?\")\\s*:\\s*(?:\\\\?\")([a-z_]+)");
    private static final Pattern ACTIVE=Pattern.compile("(?i)(?:\\\\?\")active(?:\\\\?\")\\s*:\\s*(true|false)");
    private static final Pattern ITEM_LINE=Pattern.compile("(?i)^(?:held item|pet item)\\s*:\\s*(.+)$");
    private static final Pattern TAB_XP=Pattern.compile("(?i)([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)\\s*/\\s*([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)");
    private static final Pattern MENU_PAGE=Pattern.compile("(?i)\\(?\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\)?");
    private static final Comparator<PlayerInfo> TAB_ORDER=Comparator
        .comparingInt((PlayerInfo info)->-info.getTabListOrder())
        .thenComparingInt(info->info.getGameMode()==GameType.SPECTATOR?1:0)
        .thenComparing(info->info.getTeam()==null?"":info.getTeam().getName())
        .thenComparing(info->info.getProfile().name(),String.CASE_INSENSITIVE_ORDER);
    private static final long SKYBLOCK_TRANSITION_GRACE_MILLIS=12_000L,WIDGET_AUTHORITY_GRACE_MILLIS=12_000L;
    private static final long MENU_SETTLE_MILLIS=500L;
    private static final int WIDGET_FALLBACK_TICKS=5,SKYBLOCK_CHECK_TICKS=10;
    private static final int CACHE_SCHEMA=1,MAX_CACHED_PETS=256;
    private static final Map<PetInstanceId,PetData> CACHE=new LinkedHashMap<>();
    private static PetData current;
    private static String loadedAccount;
    private static boolean persistenceDirty;
    private static Object connection;
    private static String serverAddress="";
    private static long transitionStartedMillis;
    private static long skyblockGraceUntil;
    private static int menuCooldown,skyblockCheckCooldown,widgetFallbackCooldown;
    private static boolean skyblock;
    private static PetSyncState syncState=PetSyncState.UNSYNCED;
    private static long lastSyncMillis;
    private static String lastMenuSummary="No Pets menu scanned";
    private static Object lastPetsScreen;
    private static int lastMenuFingerprint=Integer.MIN_VALUE;
    private static PetChangeSource lastChangeSource=PetChangeSource.OTHER;
    private static boolean lastChangeUsedCache;
    private static long lastChangeMillis;
    private static long stateRevision;
    private static boolean debugTracing;
    private static String lastMessage="";
    private static long lastMessageMillis;
    private static boolean containerDirty;
    private static long lastHudTraceRevision=-1;
    private static PetInstanceId pendingClickedInstance;
    private static int pendingClickedSlot=-1;
    private static PendingSelection pendingSelection;
    private static double lastLiveXpGain;
    private static String lastXpSource="NONE";
    private static long lastContainerPacketMillis;
    private static boolean menuParsedOk;
    private static boolean tabWidgetDirty=true;
    private static String lastWidgetFingerprint="";
    private static TabPetState lastWidgetState;
    private static long lastWidgetSeenMillis,lastWidgetDetectionNanos;
    private static long lastWidgetAppliedRevision=-1;

    private PetTracker() {}

    public static void tick(Minecraft client) {
        Object liveConnection=client.getConnection();
        if(liveConnection!=connection)handleConnectionChange(client,liveConnection);
        ensureAccount(client);
        if(persistenceDirty)flushToCache(client);
        boolean petsMenuOpen=client.player!=null&&client.screen instanceof AbstractContainerScreen<?> screen&&PetsMenuDetector.matches(screen);
        boolean petTrainingOverviewOpen=client.player!=null&&client.screen instanceof AbstractContainerScreen<?> screen&&isPetTrainingOverview(screen.getTitle().getString());
        if(!petsMenuOpen)clearMenuTracking();
        if(liveConnection==null||client.player==null)return;
        if(!ConfigManager.get().petDisplay.enabled)return;
        long now=System.currentTimeMillis();
        if(skyblockCheckCooldown--<=0){skyblockCheckCooldown=SKYBLOCK_CHECK_TICKS;skyblock=SkyblockSession.isActive();}
        if(skyblock&&(tabWidgetDirty||widgetFallbackCooldown--<=0)){
            tabWidgetDirty=false;widgetFallbackCooldown=WIDGET_FALLBACK_TICKS;scanTabPetWidget(client);}
        if(menuCooldown-- > 0)return;
        menuCooldown=petsMenuOpen?0:5;
        if(petTrainingOverviewOpen)scanPetTrainingOverview(client);else scanPetsMenu(client);
    }

    public static void onTabListPacket(String packetType){tabWidgetDirty=true;trace("tabInvalidated packet={}",packetType);}

    private static void handleConnectionChange(Minecraft client,Object liveConnection) {
        long now=System.currentTimeMillis();
        if(liveConnection==null) {
            connection=null;transitionStartedMillis=now;skyblockGraceUntil=now+SKYBLOCK_TRANSITION_GRACE_MILLIS;
            if(current!=null)syncState=PetSyncState.STALE;
            clearWorldTransientState();
            trace("connectionLost server={} retainingLogicalPet={} cachedPets={}",serverAddress,current==null?"none":current.instanceId().value(),CACHE.size());
            return;
        }
        String joinedAddress=currentServerAddress(client);
        // During a Hypixel server transfer Minecraft can replace the connection one tick
        // before it republishes ServerData. Treat that short blank as the same network;
        // the tab/menu authority will subsequently confirm the active pet.
        if(joinedAddress.isBlank())joinedAddress=serverAddress;
        boolean sameServer=!serverAddress.isBlank()&&serverAddress.equals(joinedAddress);
        boolean retained=sameServer&&current!=null;
        if(!sameServer&&!serverAddress.isBlank())clearLogicalSession();
        if(!joinedAddress.isBlank())serverAddress=joinedAddress;
        connection=liveConnection;menuCooldown=0;skyblockCheckCooldown=0;widgetFallbackCooldown=0;skyblockGraceUntil=now+SKYBLOCK_TRANSITION_GRACE_MILLIS;
        clearWorldTransientState();
        if(retained){syncState=PetSyncState.STALE;lastWidgetSeenMillis=now;}
        trace("connectionJoined server={} sameServer={} transitionMs={} retainedLogicalPet={} cachedPets={}",serverAddress,sameServer,
            transitionStartedMillis==0?0:now-transitionStartedMillis,current==null?"none":current.instanceId().value(),CACHE.size());
        transitionStartedMillis=0;
    }

    private static String currentServerAddress(Minecraft client) {
        var server=client.getCurrentServer();
        return server==null||server.ip==null?"":server.ip.trim().toLowerCase(Locale.ROOT);
    }

    private static void clearWorldTransientState() {
        lastPetsScreen=null;lastMenuFingerprint=Integer.MIN_VALUE;containerDirty=false;
        pendingClickedInstance=null;pendingClickedSlot=-1;lastMessage="";lastMessageMillis=0;
        menuParsedOk=false;lastContainerPacketMillis=0;lastWidgetFingerprint="";tabWidgetDirty=true;
    }

    private static void clearLogicalSession() {
        lastPetsScreen=null; lastMenuFingerprint=Integer.MIN_VALUE; containerDirty=false;
        pendingClickedInstance=null; pendingClickedSlot=-1;
        pendingSelection=null;
        lastMessage=""; lastMessageMillis=0;
        current=null;CACHE.clear();skyblock=false;syncState=PetSyncState.UNSYNCED;lastSyncMillis=0;
        lastChangeSource=PetChangeSource.OTHER;lastChangeUsedCache=false;lastChangeMillis=0;stateRevision++;
        lastWidgetState=null;lastWidgetSeenMillis=0;lastWidgetAppliedRevision=-1;lastLiveXpGain=0;lastXpSource="NONE";
    }

    public static void shutdown(Minecraft client){flushToCache(client);}
    public static void disconnect(Minecraft client){flushToCache(client);loadedAccount=null;persistenceDirty=false;clearLogicalSession();}

    private static void ensureAccount(Minecraft client) {
        if(client==null||client.player==null||client.level==null||!SkyveilCacheManager.isLoaded())return;
        String account=client.player.getUUID().toString();
        if(account.equals(loadedAccount))return;
        if(loadedAccount!=null)flushToCache(client);
        loadedAccount=account;clearLogicalSession();loadFromCache(client);persistenceDirty=false;
    }

    private static void loadFromCache(Minecraft client) {
        CompoundTag root=SkyveilCacheManager.profileSection(loadedAccount,"pets");
        if(root==null||root.getIntOr("schema",0)!=CACHE_SCHEMA)return;
        try {
            DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());
            ListTag entries=root.getListOrEmpty("entries");PetData restoredCurrent=null;
            for(int index=0;index<Math.min(entries.size(),MAX_CACHED_PETS);index++) {
                CompoundTag entry=entries.getCompoundOrEmpty(index);PetData restored=decodePet(entry,ops);
                if(restored==null)continue;CACHE.put(restored.instanceId(),restored);
                if(entry.getBooleanOr("current",false))restoredCurrent=restored;
            }
            current=restoredCurrent;
            if(current!=null){syncState=PetSyncState.STALE;stateRevision++;lastChangeSource=PetChangeSource.OTHER;lastChangeUsedCache=true;lastChangeMillis=System.currentTimeMillis();}
            LOGGER.info("Restored {} pets and active pet {} from the unified cache",CACHE.size(),current==null?"none":current.name());
        } catch(Exception exception){LOGGER.warn("Could not restore the Pet Display cache",exception);CACHE.clear();current=null;}
    }

    private static void flushToCache(Minecraft client) {
        if(!persistenceDirty||loadedAccount==null||client==null||client.level==null)return;
        try {
            DynamicOps<Tag> ops=RegistryOps.create(NbtOps.INSTANCE,client.level.registryAccess());CompoundTag root=new CompoundTag();root.putInt("schema",CACHE_SCHEMA);ListTag entries=new ListTag();
            int count=0;for(PetData data:CACHE.values()){if(count++>=MAX_CACHED_PETS)break;CompoundTag entry=encodePet(data,ops);entry.putBoolean("current",current!=null&&current.instanceId().equals(data.instanceId()));entries.add(entry);}
            root.put("entries",entries);SkyveilCacheManager.putProfileSection(loadedAccount,"pets",root);persistenceDirty=false;
        } catch(Exception exception){LOGGER.warn("Could not prepare the Pet Display cache",exception);}
    }

    private static CompoundTag encodePet(PetData data,DynamicOps<Tag> ops) {
        CompoundTag entry=new CompoundTag();entry.putString("instance",data.instanceId().value());entry.putString("source",data.instanceId().source().name());entry.putString("confidence",data.instanceId().confidence().name());
        entry.putString("internalId",safe(data.internalId()));entry.putString("name",safe(data.name()));entry.putString("rarity",data.rarity()==null?"":data.rarity().name());entry.putInt("level",data.level());entry.putBoolean("levelKnown",data.levelKnown());entry.putInt("maxLevel",data.maxLevel());
        entry.putDouble("xp",data.currentLevelXp());entry.putDouble("required",data.xpForNextLevel());entry.putBoolean("xpKnown",data.xpKnown());entry.putBoolean("maxed",data.maxed());entry.putString("petItemId",safe(data.petItemId()));entry.putString("petItemName",safe(data.petItemName()));entry.putString("petItemRarity",data.petItemRarity()==null?"":data.petItemRarity().name());entry.putInt("petItemRgb",data.petItemRgb());
        encodeStack(entry,"petIcon",data.petIcon(),ops);encodeStack(entry,"petItemIcon",data.petItemIcon(),ops);return entry;
    }

    private static PetData decodePet(CompoundTag entry,DynamicOps<Tag> ops) {
        String instance=entry.getStringOr("instance","");String name=entry.getStringOr("name","");if(instance.isBlank()||instance.length()>160||name.isBlank()||name.length()>160)return null;
        try {
            PetInstanceId id=new PetInstanceId(instance,PetInstanceId.Source.valueOf(entry.getStringOr("source","FALLBACK")),PetInstanceId.Confidence.valueOf(entry.getStringOr("confidence","UNKNOWN")));
            int maxLevel=Math.max(1,Math.min(500,entry.getIntOr("maxLevel",100))),level=Math.max(0,Math.min(maxLevel,entry.getIntOr("level",0)));
            return new PetData(id,limited(entry.getStringOr("internalId",""),160),name,rarity(entry.getStringOr("rarity","")),level,entry.getBooleanOr("levelKnown",false),maxLevel,
                Math.max(0,entry.getDoubleOr("xp",0)),Math.max(0,entry.getDoubleOr("required",0)),entry.getBooleanOr("xpKnown",false),entry.getBooleanOr("maxed",false),
                limited(entry.getStringOr("petItemId",""),160),limited(entry.getStringOr("petItemName",""),160),rarity(entry.getStringOr("petItemRarity","")),entry.getIntOr("petItemRgb",0),decodeStack(entry,"petIcon",ops),decodeStack(entry,"petItemIcon",ops));
        } catch(Exception ignored){return null;}
    }

    private static void encodeStack(CompoundTag entry,String key,ItemStack stack,DynamicOps<Tag> ops){if(stack==null||stack.isEmpty())return;ItemStack.CODEC.encodeStart(ops,stack).result().ifPresent(tag->entry.put(key,tag));}
    private static ItemStack decodeStack(CompoundTag entry,String key,DynamicOps<Tag> ops){Tag tag=entry.get(key);if(tag==null)return ItemStack.EMPTY;try{return ItemStack.CODEC.parse(ops,tag).result().orElse(ItemStack.EMPTY);}catch(Exception ignored){return ItemStack.EMPTY;}}
    private static SkyblockRarity rarity(String value){if(value==null||value.isBlank())return null;try{return SkyblockRarity.valueOf(value);}catch(Exception ignored){return null;}}
    private static String safe(String value){return value==null?"":limited(value,160);}
    private static String limited(String value,int length){return value==null?"":value.substring(0,Math.min(value.length(),length));}

    public static PetData current(){return current;}
    public static boolean inSkyblock(){return SkyblockSession.isActive();}
    public static PetSyncState syncState(){return syncState;}
    public static String debugSummary(){
        PetData data=current;
        String pet=data==null?"none":data.name()+" instance="+data.instanceId().value()+" identitySource="+data.instanceId().source()+" confidence="+data.instanceId().confidence()+" level="+(data.levelKnown()?data.level():"unknown")+" xp="+(data.xpKnown()?data.currentLevelXp()+"/"+data.xpForNextLevel()+" remaining="+data.xpRemaining():"unknown")+" item="+(data.hasPetItem()?data.petItemName():"none")+" itemId="+(data.petItemId()==null||data.petItemId().isBlank()?"unknown":data.petItemId())+" itemRarity="+(data.petItemRarity()==null?"unknown":data.petItemRarity()+"/0x"+String.format(Locale.ROOT,"%06X",data.petItemRarity().rgb()))+" head="+(data.petIcon()!=null&&!data.petIcon().isEmpty());
        boolean menuOpen=Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen&&PetsMenuDetector.matches(screen);
        String pending=pendingSelection==null?"none":pendingSelection.name()+"/L"+pendingSelection.level()+"/"+pendingSelection.rarity();
        return "state="+syncState+", revision="+stateRevision+", pet="+pet+", source="+lastChangeSource+", cached="+lastChangeUsedCache
            +", server="+serverAddress+", transition="+(transitionStartedMillis>0)+", pending="+pending+", liveXpGain="+lastLiveXpGain+", xpSource="+lastXpSource
            +", widget="+(lastWidgetState==null?"none":lastWidgetState.summary())+", widgetAge="+(lastWidgetSeenMillis==0?"never":(System.currentTimeMillis()-lastWidgetSeenMillis)+"ms")
            +", lastChange="+(lastChangeMillis==0?"never":(System.currentTimeMillis()-lastChangeMillis)+"ms ago")+", cachedPets="+cachedPets().size()+", petsMenuOpen="+menuOpen
            +", lastSync="+(lastSyncMillis==0?"never":(System.currentTimeMillis()-lastSyncMillis)+"ms ago")+", menu={"+lastMenuSummary+"}";
    }

    public static boolean toggleDebugTracing(){debugTracing=!debugTracing;lastHudTraceRevision=-1;return debugTracing;}
    public static List<String> debugCacheLines(){return cachedPets().stream().map(data->data.instanceId().value()+" ["+data.instanceId().source()+"/"+data.instanceId().confidence()+"] | "+data.name()+" | "+data.rarity()+" | Lvl "+data.level()+" | "+data.currentLevelXp()+"/"+data.xpForNextLevel()+" XP | item="+(data.hasPetItem()?data.petItemName():"none")).toList();}

    public static void onContainerPacket(int containerId,String packetType) {
        if(!ConfigManager.get().petDisplay.enabled)return;
        var client=Minecraft.getInstance();
        if(!(client.screen instanceof AbstractContainerScreen<?> screen)||!PetsMenuDetector.matches(screen)||screen.getMenu().containerId!=containerId)return;
        containerDirty=true;lastMenuFingerprint=Integer.MIN_VALUE;menuParsedOk=false;lastContainerPacketMillis=System.currentTimeMillis();menuCooldown=0;
        trace("containerUpdate type={} id={} markedDirty=true",packetType,containerId);
    }

    public static void debugHudRender(PetData data,int renderedRgb) {
        if(!debugTracing||data==null||lastHudTraceRevision==stateRevision)return;
        lastHudTraceRevision=stateRevision;
        long widgetToHudMicros=lastWidgetAppliedRevision==stateRevision&&lastWidgetDetectionNanos>0?(System.nanoTime()-lastWidgetDetectionNanos)/1_000L:-1;
        trace("hud revision={} pet={} petItem={} rarity={} storedRgb=#{} renderedRgb=#{} widgetToHudMicros={}",stateRevision,data.name(),data.petItemName(),data.petItemRarity(),hex(data.petItemRgb()),hex(renderedRgb),widgetToHudMicros);
    }

    /** Called by the container-screen click hook; the server's following slot update is authoritative. */
    public static void onPetsMenuInteraction(AbstractContainerScreen<?> screen,Slot slot) {
        if(!ConfigManager.get().petDisplay.enabled)return;
        if(!PetsMenuDetector.matches(screen))return;
        pendingClickedInstance=null;pendingClickedSlot=-1;
        if(slot!=null&&slot.hasItem()) {
            var client=Minecraft.getInstance();
            List<Component> components=Screen.getTooltipFromItem(client,slot.getItem());
            List<String> lore=components.stream().map(Component::getString).toList();
            // Page arrows, sorting, search, and visibility controls are not pet selections.
            // Remember a click only when the clicked stack is an actual parsed pet.
            Candidate clicked=parseCandidate(slot.getItem(),components,lore,slot.index);
            if(clicked!=null&&!clicked.training){pendingClickedInstance=clicked.data.instanceId();pendingClickedSlot=slot.index;}
        }
        syncState=PetSyncState.SYNCING;
        trace("petsMenuClick pendingInstance={} slot={}",pendingClickedInstance,pendingClickedSlot);
    }

    private static void scanPetsMenu(Minecraft client) {
        if(!(client.screen instanceof AbstractContainerScreen<?> screen)||!PetsMenuDetector.matches(screen))return;
        long now=System.currentTimeMillis();
        if(screen!=lastPetsScreen){lastPetsScreen=screen;lastMenuFingerprint=Integer.MIN_VALUE;menuParsedOk=false;containerDirty=true;lastContainerPacketMillis=now;}
        // Container packets can arrive as a burst. Parse once the visible page has settled instead
        // of caching a half-populated page; live widget updates are handled independently above.
        if(containerDirty&&now-lastContainerPacketMillis<MENU_SETTLE_MILLIS)return;
        int fingerprint=menuFingerprint(screen,client);
        if(menuParsedOk&&!containerDirty&&fingerprint==lastMenuFingerprint)return;
        lastMenuFingerprint=fingerprint;containerDirty=false;menuParsedOk=true;
        syncState=PetSyncState.SYNCING;
        List<Candidate> candidates=new ArrayList<>();
        String selected=null;
        for(var slot:screen.getMenu().slots) {
            if(!slot.hasItem()||slot.container==client.player.getInventory())continue;
            ItemStack stack=slot.getItem();
            List<Component> components=Screen.getTooltipFromItem(client,stack);
            List<String> lore=components.stream().map(Component::getString).toList();
            String selection=findSelectedPet(lore);
            if(selection!=null&&!selection.isBlank())selected=selection;
            Candidate candidate=parseCandidate(stack,components,lore,slot.index);
            if(candidate!=null)candidates.add(candidate);
        }
        candidates=reconcileCandidateIdentities(candidates);
        for(Candidate candidate:candidates){cache(candidate.data);tracePetData("menuParsed",candidate.data);}
        TabPetState widget=currentWidgetAuthority();
        if(widget!=null){
            Candidate detailed=bestWidgetMatch(candidates.stream().filter(candidate->!candidate.training).toList(),widget);int[] page=menuPage(screen.getTitle().getString());
            lastMenuSummary="title='"+screen.getTitle().getString()+"', page="+page[0]+"/"+page[1]+", candidates="+candidates.size()+", liveWidget="+widget.summary()+", detailMatch="+(detailed==null?"not visible":detailed.data.name());
            if(detailed!=null){PetData enriched=withWidgetState(detailed.data,widget);cache(enriched);replaceCurrent(enriched,PetChangeSource.TAB_WIDGET,true);syncState=PetSyncState.SYNCED;lastSyncMillis=now;pendingSelection=null;}
            return;
        }
        if(selected!=null&&selected.equalsIgnoreCase("none")){pendingSelection=null;replaceCurrent(null,PetChangeSource.PETS_MENU,false);syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();lastMenuSummary="title='"+screen.getTitle().getString()+"', candidates="+candidates.size()+", selected=none";return;}
        Candidate chosen=null;
        List<Candidate> selectableCandidates=candidates.stream().filter(candidate->!candidate.training).toList();
        List<Candidate> activeCandidates=selectableCandidates.stream().filter(Candidate::active).toList();
        if(activeCandidates.size()==1)chosen=activeCandidates.getFirst();
        if(chosen==null&&pendingClickedSlot>=0)for(Candidate candidate:selectableCandidates)if(candidate.slotIndex==pendingClickedSlot){chosen=candidate;break;}
        if(chosen==null&&pendingClickedInstance!=null)for(Candidate candidate:selectableCandidates)if(candidate.data.instanceId().equals(pendingClickedInstance)){chosen=candidate;break;}
        if(chosen==null&&selected!=null) {
            chosen=bestVisibleMatch(selectableCandidates,selected);
        }
        if(chosen==null&&current!=null)for(Candidate candidate:selectableCandidates)if(candidate.data.instanceId().equals(current.instanceId())){chosen=candidate;break;}
        // A live tab/chat snapshot has a deliberately temporary identity. Once its pet becomes
        // visible on a later menu page, promote the complete menu snapshot so the HUD regains
        // the real head, rarity colour, and held-item data.
        if(chosen==null&&current!=null&&current.instanceId().source()==PetInstanceId.Source.LIVE_UNRESOLVED)
            chosen=bestVisibleMatch(selectableCandidates,current.name());
        int[] page=menuPage(screen.getTitle().getString());
        lastMenuSummary="title='"+screen.getTitle().getString()+"', page="+page[0]+"/"+page[1]+", slots="+screen.getMenu().slots.size()+", candidates="+candidates.size()+", selected="+(selected==null?"unknown":selected)+", equipped="+(chosen==null?"not found":chosen.data.name());
        if(chosen!=null){cache(chosen.data);replaceCurrent(chosen.data,PetChangeSource.PETS_MENU,true);syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();pendingClickedInstance=null;pendingClickedSlot=-1;pendingSelection=null;}
    }

    /** Training pets are inventory-owned pets, not the player's currently summoned pet. */
    private static void scanPetTrainingOverview(Minecraft client) {
        if(!(client.screen instanceof AbstractContainerScreen<?> screen)||!isPetTrainingOverview(screen.getTitle().getString())||current==null)return;
        List<Candidate> trainingPets=new ArrayList<>();
        for(var slot:screen.getMenu().slots) {
            if(!slot.hasItem()||slot.container==client.player.getInventory())continue;
            ItemStack stack=slot.getItem();
            List<Component> components=Screen.getTooltipFromItem(client,stack);
            List<String> lore=components.stream().map(Component::getString).toList();
            Candidate candidate=parseCandidate(stack,components,lore,slot.index);
            if(candidate!=null){trainingPets.add(candidate);cache(candidate.data);}
        }
        List<Candidate> matches=trainingPets.stream().filter(candidate->sameTrainingPet(candidate.data,current)).toList();
        if(matches.size()==1) {
            trace("trainingPetSuppressed instance={} pet={}",matches.getFirst().data.instanceId().value(),matches.getFirst().data.name());
            pendingSelection=null;lastWidgetState=null;lastWidgetFingerprint="";
            replaceCurrent(null,PetChangeSource.PET_TRAINING,false);syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();
        }
    }

    static boolean isPetTrainingOverview(String title) {
        String normalized=(title==null?"":title).replaceAll("(?:\\u00c2)?\\u00a7.","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        return normalized.equals("pet training")||normalized.matches("pet training \\d+ \\d+");
    }

    private static boolean sameTrainingPet(PetData training,PetData active) {
        if(training.instanceId().equals(active.instanceId()))return true;
        if(training.instanceId().confidence()==PetInstanceId.Confidence.EXACT&&active.instanceId().confidence()==PetInstanceId.Confidence.EXACT)return false;
        return samePet(training.name(),active.name())&&training.level()==active.level()
            &&(training.rarity()==null||active.rarity()==null||training.rarity()==active.rarity());
    }

    private static TabPetState currentWidgetAuthority(){return lastWidgetState!=null&&System.currentTimeMillis()-lastWidgetSeenMillis<=WIDGET_AUTHORITY_GRACE_MILLIS?lastWidgetState:null;}

    private static Candidate bestWidgetMatch(List<Candidate> candidates,TabPetState widget){
        List<Candidate> matches=candidates.stream().filter(candidate->samePet(candidate.data.name(),widget.name()))
            .filter(candidate->candidate.data.level()==widget.level()).filter(candidate->widget.rarity()==null||candidate.data.rarity()==widget.rarity()).toList();
        if(matches.size()==1)return matches.getFirst();
        if(matches.size()>1&&widget.xpKnown()){PetData closest=closestXp(matches.stream().map(Candidate::data).toList(),widget.xp());if(closest!=null)for(Candidate candidate:matches)if(candidate.data.instanceId().equals(closest.instanceId()))return candidate;}
        if(matches.size()>1&&current!=null)for(Candidate candidate:matches)if(candidate.data.instanceId().equals(current.instanceId()))return candidate;
        trace("widgetMenuIdentity ambiguous pet={} level={} rarity={} visibleCandidates={}",widget.name(),widget.level(),widget.rarity(),matches.size());return null;
    }

    private static Candidate bestVisibleMatch(List<Candidate> candidates,String name) {
        List<Candidate> matches=candidates.stream().filter(candidate->samePet(candidate.data.name(),name)).toList();
        PetData signal=current;
        if(pendingSelection!=null&&samePet(pendingSelection.name,name)) {
            int level=pendingSelection.level;
            SkyblockRarity rarity=pendingSelection.rarity;
            List<Candidate> refined=matches.stream()
                .filter(candidate->level<=0||candidate.data.level()==level)
                .filter(candidate->rarity==null||candidate.data.rarity()==rarity).toList();
            if(!refined.isEmpty())matches=refined;
        } else if(signal!=null&&samePet(signal.name(),name)) {
            List<Candidate> refined=matches.stream()
                .filter(candidate->!signal.levelKnown()||candidate.data.level()==signal.level())
                .filter(candidate->signal.rarity()==null||candidate.data.rarity()==signal.rarity()).toList();
            if(!refined.isEmpty())matches=refined;
        }
        if(matches.size()==1)return matches.getFirst();
        if(matches.size()>1&&signal!=null&&signal.xpKnown()) {
            PetData closest=closestXp(matches.stream().map(Candidate::data).toList(),signal.currentLevelXp());
            if(closest!=null)for(Candidate candidate:matches)if(candidate.data.instanceId().equals(closest.instanceId()))return candidate;
        }
        trace("menuIdentity ambiguous pet={} visibleCandidates={}",name,matches.size());
        return null;
    }

    private static int[] menuPage(String title) {
        Matcher matcher=MENU_PAGE.matcher(title==null?"":title.replaceAll("(?:\\u00c2)?\\u00a7.",""));
        if(!matcher.find())return new int[]{1,1};
        return new int[]{parseInt(matcher.group(1),1),parseInt(matcher.group(2),1)};
    }

    private static void clearMenuTracking() {
        lastPetsScreen=null;
        lastMenuFingerprint=Integer.MIN_VALUE;
        containerDirty=false;
        menuParsedOk=false;
        lastContainerPacketMillis=0;
        pendingClickedInstance=null;
        pendingClickedSlot=-1;
    }

    private static int menuFingerprint(AbstractContainerScreen<?> screen,Minecraft client) {
        int hash=1;
        for(var slot:screen.getMenu().slots) {
            if(slot.container==client.player.getInventory())continue;
            hash=31*hash+slot.index;
            hash=31*hash+ItemStack.hashItemAndComponents(slot.getItem());
        }
        return hash;
    }

    private static String findSelectedPet(List<String> tooltip) {
        for(int index=0;index<tooltip.size();index++) {
            String line=tooltip.get(index).trim();
            String lower=line.toLowerCase(Locale.ROOT);
            int marker=lower.indexOf("selected pet");
            if(marker<0)marker=lower.indexOf("equipped pet");
            if(marker<0)continue;
            int colon=line.indexOf(':',marker);
            String value=colon>=0?cleanName(line.substring(colon+1)):"";
            if(!key(value).isBlank())return value;
            for(int next=index+1;next<tooltip.size();next++) {
                value=cleanName(tooltip.get(next));
                if(!key(value).isBlank())return value;
            }
        }
        return null;
    }

    private static Candidate parseCandidate(ItemStack stack,List<Component> components,List<String> lore,int slotIndex) {
        PetLevelParser.Parsed header=findPetHeader(stack,lore);
        if(header==null)return null;
        int level=header.level();
        String name=cleanName(header.name());
        String metadata=customData(stack);
        String internal=find(TYPE,metadata,"",1).toUpperCase(Locale.ROOT);
        double totalXp=parseDouble(find(EXP,metadata,"-1",1),-1);
        String heldItem=find(HELD_ITEM,metadata,"",1).toUpperCase(Locale.ROOT);
        SkyblockRarity metadataRarity=SkyblockRarity.fromLabel(find(TIER,metadata,"",1));
        SkyblockRarity rarity=ItemRarityDetector.detect(stack);
        if(rarity==null)rarity=metadataRarity;
        String itemName="";
        SkyblockRarity itemRarity=null;
        int itemRgb=0;
        boolean active=Boolean.parseBoolean(find(ACTIVE,metadata,"false",1));
        boolean training=isTrainingPetLore(lore);
        for(int index=0;index<lore.size();index++) {
            String line=lore.get(index);
            String normalized=line.trim();
            Matcher item=ITEM_LINE.matcher(normalized);
            if(item.find()) {
                itemName=cleanName(item.group(1));
                var itemHighlight=ItemRarityDetector.detectStyledTextHighlight(components.get(index),itemName);
                if(itemHighlight!=null){itemRarity=itemHighlight.rarity();itemRgb=itemHighlight.rgb();}
                trace("itemLore pet={} raw='{}' item={} flat={} detectedRarity={} detectedRgb=#{}",name,normalized,itemName,styledParts(components.get(index)),itemRarity,hex(itemRgb));
            }
            String lower=normalized.toLowerCase(Locale.ROOT);
            if(lower.contains("click to despawn")||lower.contains("currently selected")||lower.contains("currently equipped")||lower.contains("active pet"))active=true;
        }
        if(itemName.equalsIgnoreCase("none")||itemName.equalsIgnoreCase("no item"))itemName="";
        if(itemName.isBlank())itemName=PetItemResolver.displayName(heldItem);
        ItemStack itemIcon=PetItemResolver.resolve(heldItem,itemName);
        if(itemRarity==null){itemRarity=ItemRarityDetector.detect(itemIcon);if(itemRarity!=null)itemRgb=itemRarity.rgb();}
        int maxLevel=PetXpCalculator.maxLevel(internal,name);
        // A Tier Boost can change the displayed rarity without changing the XP ladder.
        var xp=PetXpCalculator.calculate(lore,internal,level,maxLevel,metadataRarity!=null?metadataRarity:rarity,totalXp);
        PetInstanceId instanceId=PetInstanceId.fromMetadata(metadata,internal,metadataRarity==null?"":metadataRarity.name(),heldItem);
        PetData data=new PetData(instanceId,internal,name,rarity,level,true,maxLevel,xp.current(),xp.required(),xp.known(),xp.maxed(),heldItem,itemName,itemRarity,itemRgb,stack.copy(),itemIcon);
        return new Candidate(data,active&&!training,training,slotIndex);
    }

    static boolean isTrainingPetLore(List<String> lore) {
        if(lore==null)return false;
        for(String raw:lore) {
            String line=(raw==null?"":raw).replaceAll("(?:\\u00c2)?\\u00a7.","").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
            if(line.contains("currently in training")||line.contains("is currently training")||line.contains("training in progress")
                ||line.contains("cancel training")||line.contains("training time remaining")||line.contains("collect trained pet"))return true;
        }
        return false;
    }

    /** Prevents UUID-less duplicate pets from overwriting one another in the session cache. */
    private static List<Candidate> reconcileCandidateIdentities(List<Candidate> candidates) {
        Map<PetInstanceId,List<Integer>> groups=new LinkedHashMap<>();
        for(int index=0;index<candidates.size();index++)groups.computeIfAbsent(candidates.get(index).data.instanceId(),ignored->new ArrayList<>()).add(index);
        List<Candidate> resolved=new ArrayList<>(candidates);
        for(var entry:groups.entrySet()) {
            PetInstanceId base=entry.getKey();List<Integer> indexes=entry.getValue();
            if(base.confidence()==PetInstanceId.Confidence.EXACT)continue;
            List<PetData> prior=CACHE.values().stream().filter(data->data.instanceId().belongsTo(base)).toList();
            Set<PetInstanceId> used=new HashSet<>();
            Map<Integer,PetInstanceId> assignments=new HashMap<>();
            if(current!=null&&current.instanceId().belongsTo(base))for(int index:indexes)if(candidates.get(index).active()){
                assignments.put(index,current.instanceId());used.add(current.instanceId());break;
            }
            for(int index:indexes) {
                if(assignments.containsKey(index))continue;
                Candidate candidate=candidates.get(index);
                PetData closest=null;double closestDistance=Double.POSITIVE_INFINITY;
                for(PetData old:prior) {
                    if(used.contains(old.instanceId()))continue;
                    double distance=identityDistance(candidate.data,old);
                    if(distance<closestDistance){closest=old;closestDistance=distance;}
                }
                if(closest!=null){assignments.put(index,closest.instanceId());used.add(closest.instanceId());}
            }
            int ordinal=0;
            for(int index:indexes) {
                PetInstanceId assigned=assignments.get(index);
                if(assigned==null) {
                    do{assigned=ordinal++==0?base:base.disambiguated(ordinal-1);}while(used.contains(assigned));
                    used.add(assigned);
                }
                Candidate candidate=candidates.get(index);
                resolved.set(index,new Candidate(withInstanceId(candidate.data,assigned),candidate.active,candidate.training,candidate.slotIndex));
            }
            if(indexes.size()>1)trace("duplicateGroup base={} count={} assigned={}",base.value(),indexes.size(),indexes.stream().map(index->resolved.get(index).data.instanceId().value()).toList());
        }
        return resolved;
    }

    private static double identityDistance(PetData fresh,PetData old) {
        double distance=Math.abs(fresh.level()-old.level())*1_000_000_000d;
        if(fresh.rarity()!=old.rarity())distance+=1_000_000_000_000d;
        if(!fresh.internalId().equals(old.internalId()))distance+=1_000_000_000_000d;
        if(!fresh.petItemId().equals(old.petItemId()))distance+=100_000_000d;
        if(fresh.xpKnown()&&old.xpKnown())distance+=Math.abs(fresh.currentLevelXp()-old.currentLevelXp());
        return distance;
    }

    private static PetData withInstanceId(PetData data,PetInstanceId instanceId) {
        return new PetData(instanceId,data.internalId(),data.name(),data.rarity(),data.level(),data.levelKnown(),data.maxLevel(),
            data.currentLevelXp(),data.xpForNextLevel(),data.xpKnown(),data.maxed(),data.petItemId(),data.petItemName(),
            data.petItemRarity(),data.petItemRgb(),data.petIcon(),data.petItemIcon());
    }

    private static PetLevelParser.Parsed findPetHeader(ItemStack stack,List<String> tooltip) {
        PetLevelParser.Parsed direct=PetLevelParser.parse(stack.getHoverName().getString());
        if(direct!=null)return direct;
        String hoverName=cleanName(stack.getHoverName().getString());
        for(String line:tooltip) {
            PetLevelParser.Parsed parsed=PetLevelParser.parse(line);
            // Some modern Hypixel menu stacks expose only "Bee" as the hover name while
            // Screen.getTooltipFromItem renders "[Lvl N] Bee". Require the identities to
            // agree so an unrelated level-looking lore line can never become the pet level.
            if(parsed!=null&&samePet(parsed.name(),hoverName))return parsed;
        }
        return null;
    }

    /** Lightweight Pets Menu renderer lookup with a tooltip fallback for modern menu stacks. */
    static int displayedMenuLevel(ItemStack stack) {
        if(stack==null||stack.isEmpty())return -1;
        PetLevelParser.Parsed direct=PetLevelParser.parse(stack.getHoverName().getString());
        if(direct!=null)return direct.level();
        Minecraft client=Minecraft.getInstance();
        List<String> tooltip=Screen.getTooltipFromItem(client,stack).stream().map(Component::getString).toList();
        PetLevelParser.Parsed parsed=findPetHeader(stack,tooltip);
        return parsed==null?-1:parsed.level();
    }

    /** Pet Menu stacks can expose their tier only in Hypixel's structured pet metadata. */
    public static SkyblockRarity displayedMenuRarity(ItemStack stack) {
        SkyblockRarity loreRarity=ItemRarityDetector.detect(stack);if(loreRarity!=null)return loreRarity;
        return metadataRarity(customData(stack));
    }

    static SkyblockRarity metadataRarity(String metadata) {
        return SkyblockRarity.fromLabel(find(TIER,metadata==null?"":metadata,"",1));
    }

    private static String customData(ItemStack stack) {
        var custom=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return custom==null?"":custom.copyTag().toString();
    }

    /** Reads Hypixel's styled Pet widget without flattening away rarity metadata. */
    private static void scanTabPetWidget(Minecraft client) {
        if(client.getConnection()==null)return;long parseStarted=System.nanoTime();
        List<TabLine> lines=client.getConnection().getListedOnlinePlayers().stream().sorted(TAB_ORDER).limit(80)
            .map(info->{Component component=info.getTabListDisplayName();return new TabLine(component==null?Component.literal(info.getProfile().name()):component);}).toList();
        for(int header=0;header<lines.size();header++){
            String headerLower=lines.get(header).plain().toLowerCase(Locale.ROOT);
            if(!(headerLower.equals("pet")||headerLower.equals("pet:")||headerLower.startsWith("pet: ")||headerLower.equals("pets")||headerLower.equals("pets:")||headerLower.startsWith("pets: ")))continue;
            PetLevelParser.Parsed pet=null;Component petComponent=null;double xp=-1,required=-1;boolean maxed=false;
            for(int index=header+1;index<Math.min(lines.size(),header+10);index++){
                TabLine tabLine=lines.get(index);String line=tabLine.plain();
                // The Pet Training widget is often immediately below the active Pet widget.
                // It owns a different pet and must never be allowed to overwrite this section.
                if(isActivePetSectionBoundary(line))break;
                PetLevelParser.Parsed parsed=PetLevelParser.parse(line);
                if(parsed!=null&&pet==null){pet=parsed;petComponent=tabLine.component();}
                if(line.toUpperCase(Locale.ROOT).contains("MAX LEVEL"))maxed=true;
                if(line.toLowerCase(Locale.ROOT).contains("xp")){Matcher fraction=TAB_XP.matcher(line);if(fraction.find()){xp=parseDisplayNumber(fraction.group(1));required=parseDisplayNumber(fraction.group(2));}}
            }
            if(pet==null)return;
            TabPetState state=new TabPetState(cleanName(pet.name()),pet.level(),widgetRarity(petComponent,pet.name()),xp,required,maxed);
            lastWidgetSeenMillis=System.currentTimeMillis();String fingerprint=state.fingerprint();if(fingerprint.equals(lastWidgetFingerprint))return;
            lastWidgetFingerprint=fingerprint;lastWidgetState=state;lastWidgetDetectionNanos=System.nanoTime();
            trace("widgetChanged state={} parseMicros={}",state.summary(),(lastWidgetDetectionNanos-parseStarted)/1_000L);
            reconcileTabPet(state);lastWidgetAppliedRevision=stateRevision;
            trace("widgetApplied state={} revision={} totalMicros={}",state.summary(),stateRevision,(System.nanoTime()-parseStarted)/1_000L);return;
        }
    }

    static boolean isActivePetSectionBoundary(String line) {
        String normalized=(line==null?"":line).replaceAll("(?:\\u00c2)?\\u00a7.","").toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        return normalized.equals("pet training")||normalized.startsWith("pet training ");
    }

    private static SkyblockRarity widgetRarity(Component component,String name){
        var highlight=ItemRarityDetector.detectStyledTextHighlight(component,name);if(highlight!=null)return highlight.rarity();
        // A composite line's root often owns the gray level prefix while a child owns the pet
        // color. Only use the root as a fallback when there are no styled child components.
        if(component!=null&&component.getSiblings().isEmpty()){var color=component.getStyle().getColor();if(color!=null)return SkyblockRarity.fromRgb(color.getValue());}return null;
    }

    private static void reconcileTabPet(TabPetState tabPet) {
        SkyblockRarity pendingRarity=pendingSelection!=null&&samePet(pendingSelection.name,tabPet.name())?pendingSelection.rarity:null;
        SkyblockRarity rarity=tabPet.rarity()!=null?tabPet.rarity():pendingRarity;
        List<PetData> matches=findCached(tabPet.name(),tabPet.level(),rarity);
        PetData active=null;
        if(matches.size()==1)active=matches.getFirst();
        else if(matches.size()>1&&tabPet.xpKnown())active=closestXp(matches,tabPet.xp());
        // The tab widget changes to the new level before any menu snapshot can. If there is no
        // pending equip event and the active type did not change, this is the same instance
        // crossing a level boundary; keep its authoritative icon/rarity/item metadata instead
        // of downgrading it to a LIVE_UNRESOLVED placeholder.
        if(active==null&&pendingSelection==null&&current!=null&&samePet(current.name(),tabPet.name())&&(rarity==null||current.rarity()==rarity)) {
            active=current;
            if(current.level()!=tabPet.level())trace("tabLevelTransition instance={} oldLevel={} newLevel={}",current.instanceId().value(),current.level(),tabPet.level());
        }
        if(active==null&&current!=null&&current.instanceId().source()==PetInstanceId.Source.LIVE_UNRESOLVED&&samePet(current.name(),tabPet.name()))active=current;
        if(active==null) {
            if(matches.size()>1)trace("tabIdentity ambiguous pet={} level={} candidates={} xp={}",tabPet.name(),tabPet.level(),matches.size(),tabPet.xp());
            else setUnresolvedPet(tabPet.name(),tabPet.level(),rarity,PetChangeSource.TAB_WIDGET,matches.size());
            return;
        }
        pendingSelection=null;
        if(tabPet.maxed()) {
            applyDirectTabXp(active,tabPet);
            return;
        }
        if(!tabPet.xpKnown()) {
            PetData updated=withWidgetState(active,tabPet);if(!sameSnapshot(current,updated))replaceCurrent(updated,PetChangeSource.TAB_WIDGET,true);
            syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();return;
        }
        applyDirectTabXp(active,tabPet);
    }

    private static PetData closestXp(List<PetData> matches,double xp) {
        PetData closest=null;double best=Double.POSITIVE_INFINITY,second=Double.POSITIVE_INFINITY;
        for(PetData candidate:matches) {
            if(!candidate.xpKnown())continue;
            double distance=Math.abs(candidate.currentLevelXp()-xp);
            if(distance<best){second=best;best=distance;closest=candidate;}else if(distance<second)second=distance;
        }
        return closest!=null&&best+.01<second?closest:null;
    }

    private static void applyDirectTabXp(PetData active,TabPetState tabPet) {
        double xp=tabPet.maxed()?0:tabPet.xp(),required=tabPet.maxed()?0:tabPet.required();SkyblockRarity rarity=tabPet.rarity()!=null?tabPet.rarity():active.rarity();
        if(active.level()==tabPet.level()&&active.rarity()==rarity&&active.xpKnown()&&Math.abs(active.currentLevelXp()-xp)<.01&&Math.abs(active.xpForNextLevel()-required)<.01) {
            if(current==null||!current.instanceId().equals(active.instanceId()))replaceCurrent(active,PetChangeSource.TAB_WIDGET,true);
            syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();pendingSelection=null;return;
        }
        double gain=active.level()==tabPet.level()&&active.xpKnown()?Math.max(0,xp-active.currentLevelXp()):0;
        boolean maxed=tabPet.maxed()||tabPet.level()>=active.maxLevel();
        PetData updated=new PetData(active.instanceId(),active.internalId(),active.name(),rarity,tabPet.level(),true,active.maxLevel(),maxed?0:xp,maxed?0:required,true,maxed,active.petItemId(),active.petItemName(),active.petItemRarity(),active.petItemRgb(),active.petIcon(),active.petItemIcon());
        cache(updated);lastLiveXpGain=gain;lastXpSource="TAB_WIDGET";
        trace("directPetXp instance={} oldLevel={} newLevel={} oldXp={} newXp={} required={} gain={}",updated.instanceId().value(),active.level(),updated.level(),active.currentLevelXp(),updated.currentLevelXp(),updated.xpForNextLevel(),gain);
        replaceCurrent(updated,PetChangeSource.TAB_WIDGET,true);
        syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();pendingSelection=null;
    }

    private static double parseDisplayNumber(String value) {
        String normalized=value.replace(",","").replaceAll("\\s+","").toLowerCase(Locale.ROOT);double multiplier=1;
        if(normalized.endsWith("k")){multiplier=1_000;normalized=normalized.substring(0,normalized.length()-1);}
        else if(normalized.endsWith("m")){multiplier=1_000_000;normalized=normalized.substring(0,normalized.length()-1);}
        else if(normalized.endsWith("b")){multiplier=1_000_000_000;normalized=normalized.substring(0,normalized.length()-1);}
        return parseDouble(normalized,-1)*multiplier;
    }

    public static void onSystemMessage(Component component,boolean overlay){if(!overlay)receiveLiveMessage("SYSTEM_PACKET",component);}

    private static void receiveLiveMessage(String eventType,Component component) {
        if(!ConfigManager.get().petDisplay.enabled)return;
        String message=component.getString().trim();
        long now=System.currentTimeMillis();
        trace("liveEvent type={} raw='{}'",eventType,message);
        if(message.equals(lastMessage)&&now-lastMessageMillis<500)return;
        lastMessage=message;lastMessageMillis=now;
        if(message.toLowerCase(Locale.ROOT).contains("you despawned your ")){pendingSelection=null;lastWidgetState=null;lastWidgetFingerprint="";replaceCurrent(null,PetChangeSource.CHAT,false);syncState=PetSyncState.SYNCED;lastSyncMillis=System.currentTimeMillis();return;}
        Matcher match=SUMMON.matcher(message);
        PetChangeSource source=PetChangeSource.CHAT;
        if(!match.find()){match=AUTOPET.matcher(message);source=PetChangeSource.AUTOPET;if(!match.find()){trace("liveParser matched=false");return;}}
        int level=match.group(1)==null?-1:parseInt(match.group(1),-1);
        String name=cleanName(match.group(2));
        trace("liveParser matched=true source={} detectedPet={} level={}",source,name,level);
        var highlight=ItemRarityDetector.detectStyledTextHighlight(component,name);
        SkyblockRarity rarity=highlight==null?null:highlight.rarity();
        trace("liveParser rarity={} styled={}",rarity,styledParts(component));
        setActivePet(name,level,rarity,source);
    }

    private static void setActivePet(String name,int level,SkyblockRarity rarity,PetChangeSource source) {
        List<PetData> matches=findCached(name,level,rarity);
        PetData cached=matches.size()==1?matches.getFirst():null;
        syncState=cached==null?PetSyncState.STALE:PetSyncState.SYNCED;
        if(cached!=null) {
            pendingSelection=null;
            replaceCurrent(level>0&&level!=cached.level()?withLevel(cached,level):cached,source,true);
            return;
        }
        pendingSelection=new PendingSelection(name,level,rarity,source,System.currentTimeMillis());
        if(current==null||!samePet(current.name(),name))setUnresolvedPet(name,level,rarity,source,matches.size());
        trace("liveIdentity unresolved pet={} level={} candidateCount={}",name,level,matches.size());
    }

    private static void setUnresolvedPet(String name,int level,SkyblockRarity rarity,PetChangeSource source,int candidateCount) {
        int maxLevel=PetXpCalculator.maxLevel("",name);boolean levelKnown=level>0;
        PetInstanceId unresolved=new PetInstanceId("unresolved:"+key(name)+":"+Math.max(level,0),PetInstanceId.Source.LIVE_UNRESOLVED,candidateCount==0?PetInstanceId.Confidence.UNKNOWN:PetInstanceId.Confidence.PARTIAL);
        replaceCurrent(new PetData(unresolved,"",name,rarity,levelKnown?level:0,levelKnown,maxLevel,0,0,false,levelKnown&&level>=maxLevel,"","",null,0,new ItemStack(Items.PLAYER_HEAD),ItemStack.EMPTY),source,false);
    }

    private static void cache(PetData data) {
        if(data==null)return;PetData merged=withCachedVisuals(data,CACHE.get(data.instanceId()));PetData old=CACHE.put(data.instanceId(),merged);
        if(old==null||!sameSnapshot(old,merged))persistenceDirty=true;
        while(CACHE.size()>MAX_CACHED_PETS){PetInstanceId oldest=CACHE.keySet().stream().filter(id->current==null||!id.equals(current.instanceId())).findFirst().orElse(null);if(oldest==null)break;CACHE.remove(oldest);}
    }

    private static List<PetData> findCached(String name,int level,SkyblockRarity rarity) {
        return CACHE.values().stream().filter(data->samePet(data.name(),name))
            .filter(data->level<=0||data.level()==level)
            .filter(data->rarity==null||data.rarity()==rarity).toList();
    }

    private static void replaceCurrent(PetData replacement,PetChangeSource source,boolean cached) {
        if(replacement!=null)replacement=withCachedVisuals(replacement,CACHE.get(replacement.instanceId()));
        if(sameSnapshot(current,replacement)){lastChangeSource=source;lastChangeUsedCache=cached;return;}
        String old=current==null?"none":current.name();
        current=replacement;
        persistenceDirty=true;
        stateRevision++;
        lastChangeSource=source;lastChangeUsedCache=cached;lastChangeMillis=System.currentTimeMillis();
        trace("activeChanged old={} new={} source={} cached={} revision={}",old,replacement==null?"none":replacement.name(),source,cached,stateRevision);
        if(replacement!=null)tracePetData("activeStored",replacement);
    }

    private static PetData withCachedVisuals(PetData data,PetData cached) {
        if(data==null||cached==null)return data;
        ItemStack petIcon=data.petIcon()==null||data.petIcon().isEmpty()?cached.petIcon():data.petIcon();
        boolean sameItem=data.hasPetItem()&&(safe(data.petItemId()).equals(safe(cached.petItemId()))||safe(data.petItemName()).equalsIgnoreCase(safe(cached.petItemName())));
        ItemStack itemIcon=(data.petItemIcon()==null||data.petItemIcon().isEmpty())&&sameItem?cached.petItemIcon():data.petItemIcon();
        if(sameStack(petIcon,data.petIcon())&&sameStack(itemIcon,data.petItemIcon()))return data;
        return new PetData(data.instanceId(),data.internalId(),data.name(),data.rarity(),data.level(),data.levelKnown(),data.maxLevel(),data.currentLevelXp(),data.xpForNextLevel(),data.xpKnown(),data.maxed(),data.petItemId(),data.petItemName(),data.petItemRarity(),data.petItemRgb(),petIcon,itemIcon);
    }

    private static PetData withLevel(PetData data,int level) {
        boolean maxed=level>=data.maxLevel();
        return new PetData(data.instanceId(),data.internalId(),data.name(),data.rarity(),level,true,data.maxLevel(),0,0,false,maxed,data.petItemId(),data.petItemName(),data.petItemRarity(),data.petItemRgb(),data.petIcon(),data.petItemIcon());
    }

    private static PetData withWidgetState(PetData data,TabPetState widget){
        boolean maxed=widget.maxed()||widget.level()>=data.maxLevel();boolean directXp=widget.xpKnown()&&!maxed;
        boolean preserveXp=!directXp&&!maxed&&data.level()==widget.level()&&data.xpKnown();
        double xp=directXp?widget.xp():preserveXp?data.currentLevelXp():0,required=directXp?widget.required():preserveXp?data.xpForNextLevel():0;
        return new PetData(data.instanceId(),data.internalId(),data.name(),widget.rarity()!=null?widget.rarity():data.rarity(),widget.level(),true,data.maxLevel(),maxed?0:xp,maxed?0:required,directXp||preserveXp||maxed,maxed,data.petItemId(),data.petItemName(),data.petItemRarity(),data.petItemRgb(),data.petIcon(),data.petItemIcon());
    }

    private static boolean sameSnapshot(PetData left,PetData right){
        if(left==right)return true;if(left==null||right==null)return false;
        return left.instanceId().equals(right.instanceId())&&left.internalId().equals(right.internalId())&&left.name().equals(right.name())&&left.rarity()==right.rarity()
            &&left.level()==right.level()&&left.levelKnown()==right.levelKnown()&&left.maxLevel()==right.maxLevel()&&Math.abs(left.currentLevelXp()-right.currentLevelXp())<.01
            &&Math.abs(left.xpForNextLevel()-right.xpForNextLevel())<.01&&left.xpKnown()==right.xpKnown()&&left.maxed()==right.maxed()
            &&left.petItemId().equals(right.petItemId())&&left.petItemName().equals(right.petItemName())&&left.petItemRarity()==right.petItemRarity()&&left.petItemRgb()==right.petItemRgb()
            &&sameStack(left.petIcon(),right.petIcon())&&sameStack(left.petItemIcon(),right.petItemIcon());
    }

    private static boolean sameStack(ItemStack left,ItemStack right){if(left==right)return true;if(left==null||right==null)return false;return ItemStack.isSameItemSameComponents(left,right)&&left.getCount()==right.getCount();}

    private static List<PetData> cachedPets(){return List.copyOf(CACHE.values());}
    private static void tracePetData(String stage,PetData data){trace("{} instance={} identity={}/{} pet={} petRarity={} level={} xp={}/{} item={} itemId={} itemRarity={} itemRgb=#{} head={} itemIcon={}",stage,data.instanceId().value(),data.instanceId().source(),data.instanceId().confidence(),data.name(),data.rarity(),data.level(),data.currentLevelXp(),data.xpForNextLevel(),data.petItemName(),data.petItemId(),data.petItemRarity(),hex(data.petItemRgb()),data.petIcon()!=null&&!data.petIcon().isEmpty(),data.petItemIcon()!=null&&!data.petItemIcon().isEmpty());}
    private static String styledParts(Component line){return line.toFlatList().stream().map(part->"['"+part.getString()+"' #"+(part.getStyle().getColor()==null?"none":hex(part.getStyle().getColor().getValue()))+"]").toList().toString();}
    private static String hex(int rgb){return String.format(Locale.ROOT,"%06X",rgb&0xFFFFFF);}
    private static void trace(String message,Object...args){if(debugTracing)LOGGER.info("[Skyveil PetDebug] "+message,args);}

    private static String find(Pattern pattern,String input,String fallback,int group){Matcher matcher=pattern.matcher(input);return matcher.find()?matcher.group(group):fallback;}
    private static int parseInt(String value,int fallback){try{return Integer.parseInt(value);}catch(Exception ignored){return fallback;}}
    private static double parseDouble(String value,double fallback){try{return Double.parseDouble(value);}catch(Exception ignored){return fallback;}}
    private static String cleanName(String value){
        String plain=value.replaceAll("(?:\\u00c2)?\\u00a7.","").trim();
        PetLevelParser.Parsed parsed=PetLevelParser.parse(plain);
        if(parsed!=null)plain=parsed.name();
        return plain.replaceAll("(?i)\\s*VIEW RULE\\s*$","").replaceAll("[!.]+$","").trim();
    }
    private static String key(String value){return cleanName(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static boolean samePet(String left,String right){String a=key(left),b=key(right);return !a.isBlank()&&a.equals(b);}
    private record TabLine(Component component){String plain(){return component.getString().replaceAll("(?:\\u00c2)?\\u00a7.","").trim();}}
    private record TabPetState(String name,int level,SkyblockRarity rarity,double xp,double required,boolean maxed){
        boolean xpKnown(){return !maxed&&xp>=0&&required>0;}
        String fingerprint(){return key(name)+'|'+level+'|'+rarity+'|'+(xpKnown()?xp:"?")+'|'+(xpKnown()?required:"?")+'|'+maxed;}
        String summary(){return name+"/L"+level+"/"+(rarity==null?"?":rarity)+"/"+(maxed?"MAX":xpKnown()?xp+"/"+required:"XP?");}
    }
    private record Candidate(PetData data,boolean active,boolean training,int slotIndex){}
    private record PendingSelection(String name,int level,SkyblockRarity rarity,PetChangeSource source,long createdMillis){}
    private enum PetChangeSource {PETS_MENU,PET_TRAINING,CHAT,AUTOPET,TAB_WIDGET,OTHER}
}
