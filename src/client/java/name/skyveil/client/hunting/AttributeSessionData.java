package name.skyveil.client.hunting;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Account-scoped progression and Hunting Box quantities, persisted between game launches. */
final class AttributeSessionData {
    // Maintained shard catalog: 320 entries, excluding the non-syphonable Chameleon.
    // Rainbug remains part of the 320; the non-syphonable Chameleon is excluded.
    static final int TOTAL_CONSUMABLE_ATTRIBUTES=AttributeShardResolver.catalogSize();
    private static Object scope;
    private static String persistentAccount;
    private static final Map<String,Long> OWNED=new HashMap<>();
    private static final Map<Integer,Set<String>> OWNED_PAGE_PRODUCTS=new HashMap<>();
    private static final Map<String,AttributeMenuParser.Parsed> PROGRESS=new HashMap<>();
    private static final Set<Integer> VISITED_PAGES=new HashSet<>();
    private static int totalPages=1;
    private static long revision;
    private AttributeSessionData(){}
    static void ensureWorld(){Minecraft client=Minecraft.getInstance();if(client==null||client.player==null)return;ensurePersistentScope(client.player.getUUID().toString());}
    static void ensureScope(Object current){if(current!=scope||persistentAccount!=null){scope=current;persistentAccount=null;clear();}}
    private static void ensurePersistentScope(String account){if(account.equals(persistentAccount))return;scope=account;persistentAccount=account;clear();restore(AttributeProgressStore.load(account));}
    private static void clear(){OWNED.clear();OWNED_PAGE_PRODUCTS.clear();PROGRESS.clear();VISITED_PAGES.clear();totalPages=1;revision++;}
    private static void restore(AttributeProgressStore.Snapshot snapshot){
        OWNED.putAll(snapshot.owned());VISITED_PAGES.addAll(snapshot.visitedPages());totalPages=Math.max(1,snapshot.totalPages());
        for(var saved:snapshot.rows()){long owned=OWNED.getOrDefault("ATTRIBUTE:"+saved.subtype(),0L);var row=AttributeMenuParser.restore(saved.subtype(),saved.tier(),saved.toNext(),saved.ownership(),saved.sourceName(),owned);if(row!=null)PROGRESS.put(row.key(),row);}revision++;
    }
    static long owned(String key){ensureWorld();return key==null?0:OWNED.getOrDefault(key,0L);}
    static void putOwned(String key,long amount){
        ensureWorld();if(putOwnedInternal(key,amount))save();
    }
    private static boolean putOwnedInternal(String key,long amount){
        if(key==null||key.isBlank())return false;Long before=OWNED.put(key,Math.max(0,amount));
        PROGRESS.replaceAll((progressKey,row)->key.equals(row.key())?AttributeMenuParser.withOwned(row,amount):row);
        if(before==null||before!=Math.max(0,amount)){revision++;return true;}return false;
    }
    static void observeOwnedPage(int page,Map<String,Long> quantities){
        ensureWorld();boolean changed=false;Set<String> previous=OWNED_PAGE_PRODUCTS.put(page,Set.copyOf(quantities.keySet()));
        if(previous!=null)for(String product:previous)if(!quantities.containsKey(product))changed|=putOwnedInternal(product,0);
        for(var entry:quantities.entrySet())changed|=putOwnedInternal(entry.getKey(),entry.getValue());if(changed)save();
    }
    static boolean observeSyphon(String displayedAttribute,long amount,int tier,int toNext){
        ensureWorld();var identity=AttributeShardResolver.resolveDisplayedName(displayedAttribute);if(identity==null||amount<=0)return false;
        String subtype=identity.structuredSubtype(),key="ATTRIBUTE:"+subtype;long owned=Math.max(0,OWNED.getOrDefault(key,0L)-amount);OWNED.put(key,owned);
        AttributeMenuParser.Ownership ownership=tier>=AttributeProgression.MAX_TIER?AttributeMenuParser.Ownership.MAXED:AttributeMenuParser.Ownership.OWNED;var previous=PROGRESS.get(key);
        AttributeMenuParser.Parsed updated=previous==null?AttributeMenuParser.restore(subtype,tier,toNext,ownership,"",owned):AttributeMenuParser.withProgress(previous,tier,toNext,ownership,owned);
        if(updated==null)return false;PROGRESS.put(key,updated);revision++;return true;
    }
    static void flushSyphonUpdates(){ensureWorld();save();}
    static boolean observe(AttributeMenuDetector.Page page,Iterable<AttributeMenuParser.Parsed> parsed){
        ensureWorld();java.util.ArrayList<AttributeMenuParser.Parsed> rows=new java.util.ArrayList<>();if(parsed!=null)parsed.forEach(rows::add);
        int previousPages=totalPages;totalPages=Math.max(totalPages,page.total());if(totalPages!=previousPages)revision++;
        // A transient empty page is a loading state, not a completed observation.
        if(rows.isEmpty())return false;
        boolean pageAdded=VISITED_PAGES.add(page.current());int before=PROGRESS.hashCode();mergeObservations(PROGRESS,rows);if(PROGRESS.hashCode()!=before||pageAdded){revision++;save();}return true;
    }
    static void mergeObservations(Map<String,AttributeMenuParser.Parsed> target,Iterable<AttributeMenuParser.Parsed> parsed){for(var row:parsed){
        AttributeMenuParser.Parsed previous=target.get(row.key());
        // Never let an incomplete/loading representation erase a proven state.
        if(previous!=null&&row.ownership()==AttributeMenuParser.Ownership.UNKNOWN&&previous.ownership()!=AttributeMenuParser.Ownership.UNKNOWN)continue;
        target.put(row.key(),row);
    }}
    static Map<String,AttributeMenuParser.Parsed> progress(){ensureWorld();return Map.copyOf(PROGRESS);}
    static int visitedPages(){ensureWorld();return VISITED_PAGES.size();}
    static int totalPages(){ensureWorld();return totalPages;}
    static boolean complete(){ensureWorld();return PROGRESS.size()>=TOTAL_CONSUMABLE_ATTRIBUTES;}
    static long revision(){ensureWorld();return revision;}
    private static void save(){if(persistentAccount==null)return;ArrayList<AttributeProgressStore.SavedRow> rows=new ArrayList<>();for(var row:PROGRESS.values()){String subtype=row.key().startsWith("ATTRIBUTE:")?row.key().substring(10):"";if(!subtype.isBlank())rows.add(new AttributeProgressStore.SavedRow(subtype,row.tier(),row.toNext(),row.ownership(),row.sourceName()));}AttributeProgressStore.save(persistentAccount,new AttributeProgressStore.Snapshot(totalPages,new ArrayList<>(VISITED_PAGES),Map.copyOf(OWNED),List.copyOf(rows)));}
}
