package name.skyveil.client.hunting;

import com.google.gson.Gson;
import name.skyveil.client.cache.SkyveilCacheManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Account-scoped Attribute snapshots held in memory and serialized only at client shutdown. */
public final class AttributeProgressStore {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-attribute-cache");
    private static final Path LEGACY_PATH=FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("attribute_progress.json");
    private static final int SCHEMA=1,MAX_ROWS=400,MAX_OWNED=640,MAX_PAGES=64;
    private static final Map<String,Snapshot> PENDING=new LinkedHashMap<>();
    private static CompletableFuture<Void> migration=CompletableFuture.completedFuture(null);
    private static boolean initializationStarted;
    private AttributeProgressStore(){}

    public static synchronized void initialize(){if(initializationStarted)return;initializationStarted=true;migration=CompletableFuture.runAsync(()->{SkyveilCacheManager.awaitLoaded();migrateLegacy();},command->Thread.startVirtualThread(command));}
    static synchronized Snapshot load(String account){Snapshot pending=PENDING.get(account);if(pending!=null)return pending;return decode(SkyveilCacheManager.profileSection(account,"attributes"));}
    static synchronized void save(String account,Snapshot snapshot){if(account==null||account.isBlank()||snapshot==null)return;PENDING.put(account,validated(snapshot));}
    public static void flush(){migration.join();synchronized(AttributeProgressStore.class){for(var entry:PENDING.entrySet())SkyveilCacheManager.putProfileSection(entry.getKey(),"attributes",encode(entry.getValue()));PENDING.clear();}}

    private static Snapshot decode(CompoundTag tag){
        if(tag==null||tag.getIntOr("schema",0)!=SCHEMA||tag.getIntOr("catalogSize",0)!=AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES)return Snapshot.empty();
        int totalPages=Math.max(1,Math.min(MAX_PAGES,tag.getIntOr("totalPages",1)));ArrayList<Integer> visited=new ArrayList<>();for(int page:tag.getIntArray("visitedPages").orElse(new int[0]))if(page>0&&page<=totalPages&&!visited.contains(page))visited.add(page);
        HashMap<String,Long> owned=new HashMap<>();ListTag savedOwned=tag.getListOrEmpty("owned");for(int index=0;index<Math.min(savedOwned.size(),MAX_OWNED);index++){CompoundTag value=savedOwned.getCompoundOrEmpty(index);String key=value.getStringOr("key","");long amount=value.getLongOr("amount",0);if(validText(key,100)&&amount>=0)owned.put(key,amount);}
        ArrayList<SavedRow> rows=new ArrayList<>();ListTag savedRows=tag.getListOrEmpty("rows");for(int index=0;index<Math.min(savedRows.size(),MAX_ROWS);index++){CompoundTag value=savedRows.getCompoundOrEmpty(index);String subtype=value.getStringOr("subtype",""),ownership=value.getStringOr("ownership","");if(!validText(subtype,100))continue;AttributeMenuParser.Ownership parsed;try{parsed=AttributeMenuParser.Ownership.valueOf(ownership);}catch(Exception ignored){continue;}rows.add(new SavedRow(subtype,Math.max(0,value.getIntOr("tier",0)),Math.max(0,value.getIntOr("toNext",0)),parsed,limited(value.getStringOr("sourceName",""),120)));}
        return validated(new Snapshot(totalPages,visited,owned,rows));
    }

    private static CompoundTag encode(Snapshot snapshot){Snapshot safe=validated(snapshot);CompoundTag tag=new CompoundTag();tag.putInt("schema",SCHEMA);tag.putInt("catalogSize",AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES);tag.putInt("totalPages",safe.totalPages());tag.putIntArray("visitedPages",safe.visitedPages().stream().mapToInt(Integer::intValue).toArray());ListTag owned=new ListTag();for(var entry:safe.owned().entrySet()){CompoundTag value=new CompoundTag();value.putString("key",entry.getKey());value.putLong("amount",entry.getValue());owned.add(value);}tag.put("owned",owned);ListTag rows=new ListTag();for(SavedRow row:safe.rows()){CompoundTag value=new CompoundTag();value.putString("subtype",row.subtype());value.putInt("tier",row.tier());value.putInt("toNext",row.toNext());value.putString("ownership",row.ownership().name());value.putString("sourceName",limited(row.sourceName(),120));rows.add(value);}tag.put("rows",rows);return tag;}
    private static Snapshot validated(Snapshot source){int pages=Math.max(1,Math.min(MAX_PAGES,source.totalPages()));List<Integer> visited=source.visitedPages()==null?List.of():source.visitedPages().stream().filter(page->page!=null&&page>0&&page<=pages).distinct().limit(MAX_PAGES).toList();LinkedHashMap<String,Long> owned=new LinkedHashMap<>();if(source.owned()!=null)for(var entry:source.owned().entrySet())if(owned.size()<MAX_OWNED&&validText(entry.getKey(),100)&&entry.getValue()!=null&&entry.getValue()>=0)owned.put(entry.getKey(),entry.getValue());ArrayList<SavedRow> rows=new ArrayList<>();if(source.rows()!=null)for(SavedRow row:source.rows())if(rows.size()<MAX_ROWS&&row!=null&&validText(row.subtype(),100)&&row.ownership()!=null)rows.add(new SavedRow(row.subtype(),Math.max(0,row.tier()),Math.max(0,row.toNext()),row.ownership(),limited(row.sourceName(),120)));return new Snapshot(pages,List.copyOf(visited),Map.copyOf(owned),List.copyOf(rows));}

    private static void migrateLegacy(){if(!Files.isRegularFile(LEGACY_PATH))return;try{if(Files.size(LEGACY_PATH)>8L*1024L*1024L)throw new IllegalStateException("legacy Attribute cache is oversized");Root root;try(Reader reader=Files.newBufferedReader(LEGACY_PATH)){root=new Gson().fromJson(reader,Root.class);}if(root!=null&&root.schema==SCHEMA&&root.profiles!=null)for(var entry:root.profiles.entrySet()){Profile profile=entry.getValue();if(profile==null||profile.catalogSize!=AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES||SkyveilCacheManager.profileSection(entry.getKey(),"attributes")!=null)continue;Snapshot snapshot=new Snapshot(profile.totalPages,profile.visitedPages,profile.owned,profile.rows);SkyveilCacheManager.putProfileSection(entry.getKey(),"attributes",encode(snapshot));}SkyveilCacheManager.registerObsolete(LEGACY_PATH);SkyveilCacheManager.registerObsolete(LEGACY_PATH.resolveSibling("attribute_progress.json.tmp"));}catch(Exception exception){LOGGER.warn("Could not migrate legacy Attribute Progress cache",exception);}}
    private static boolean validText(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max;}
    private static String limited(String value,int max){if(value==null)return "";return value.length()<=max?value:value.substring(0,max);}

    record Snapshot(int totalPages,List<Integer> visitedPages,Map<String,Long> owned,List<SavedRow> rows){static Snapshot empty(){return new Snapshot(1,List.of(),Map.of(),List.of());}}
    record SavedRow(String subtype,int tier,int toNext,AttributeMenuParser.Ownership ownership,String sourceName){}
    private static final class Root{int schema=SCHEMA;Map<String,Profile> profiles=new HashMap<>();}
    private static final class Profile{int catalogSize;int totalPages=1;List<Integer> visitedPages=new ArrayList<>();Map<String,Long> owned=new HashMap<>();List<SavedRow> rows=new ArrayList<>();}
}
