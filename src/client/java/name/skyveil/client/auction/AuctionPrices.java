package name.skyveil.client.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import name.skyveil.client.bazaar.BazaarTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One bounded, immutable market snapshot shared by all tooltip consumers. */
public final class AuctionPrices {
    private static final org.slf4j.Logger LOGGER=LoggerFactory.getLogger("skyveil-auction-prices");
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final AtomicBoolean FETCHING=new AtomicBoolean();
    private static volatile Snapshot snapshot=new Snapshot(Map.of(),0);
    private static volatile long nextAttempt;
    private static final long REFRESH_INTERVAL=300_000;
    private static final AuctionHistory HISTORY=new AuctionHistory();
    private static final AtomicBoolean INITIALIZED=new AtomicBoolean();
    public static void initialize(){
        if(!INITIALIZED.compareAndSet(false,true))return;
        Thread.startVirtualThread(()->{
            name.skyveil.client.cache.SkyveilCacheManager.awaitLoaded();
            synchronized(HISTORY){
                HISTORY.load(name.skyveil.client.cache.SkyveilCacheManager.globalSection("auction_history"),System.currentTimeMillis());
            }
        });
    }
    /** Encode history once at shutdown, never while a tooltip is waiting on its lock. */
    public static void flushHistory(){
        if(!INITIALIZED.get())return;
        name.skyveil.client.cache.SkyveilCacheManager.awaitLoaded();
        try{
            net.minecraft.nbt.CompoundTag encoded;
            synchronized(HISTORY){encoded=HISTORY.saveForCache();}
            name.skyveil.client.cache.SkyveilCacheManager.putGlobalSection("auction_history",encoded);
        }catch(RuntimeException error){LOGGER.warn("Could not save auction history",error);}
    }
    public static long revision(){return snapshot.generation();}
    public static Double averageQuote(String key){
        ensureFresh();Snapshot current=snapshot;
        return System.currentTimeMillis()-current.received()>900_000?null:current.averages().get(key);
    }
    public static AuctionHistory.Average threeDayAverage(CompoundTag extra){
        ensureFresh();
        return similarAverage(snapshot,HISTORY,extra,System.currentTimeMillis());
    }
    // Old history alone cannot justify an estimate when today's matching market is empty.
    static AuctionHistory.Average similarAverage(Snapshot current,AuctionHistory history,CompoundTag extra,long now){
        String key=AuctionSimilarity.key(extra);
        if(now-current.received()>900_000||key.isEmpty()||!current.similar().containsKey(key))return null;
        synchronized(history){return history.average(key,now);}
    }
    private static volatile String lastFailure="none";
    private AuctionPrices(){}

    /**
     * Schedules a refresh when a consumer needs prices; never waits for HTTP.
     * The retry window also covers failures, preventing repeated tooltip calls
     * from turning a missing quote into a burst of downloads.
     */
    public static void ensureFresh(){
        if(System.currentTimeMillis()<nextAttempt||!FETCHING.compareAndSet(false,true))return;
        // Claim the retry window before starting the worker, including failed initial downloads.
        nextAttempt=System.currentTimeMillis()+REFRESH_INTERVAL;
        Thread.ofPlatform().daemon().priority(Thread.MIN_PRIORITY).name("Skyveil-Auction-Refresh").start(AuctionPrices::refresh);
    }

    public static String diagnostic(){
        Snapshot current=snapshot;
        long age=current.received()==0?-1:Math.max(0,(System.currentTimeMillis()-current.received())/1000);
        return "prices="+current.prices().size()+", age="+age+"s, fetching="+FETCHING.get()
            +", last error="+lastFailure;
    }
    public static boolean isLoading(){return FETCHING.get()&&snapshot.prices().isEmpty();}
    public static Double quote(String key){
        ensureFresh();
        Snapshot current=snapshot;
        return System.currentTimeMillis()-current.received()>900_000?null:current.prices().get(key);
    }

    /**
     * Returns the mean of up to five cheapest unmodified offers, or null when
     * absent/stale. Craft ingredients use this to avoid charging upgrades twice.
     */
    public static Double unmodifiedQuote(String key){
        ensureFresh();Snapshot current=snapshot;
        return System.currentTimeMillis()-current.received()>900_000?null:current.unmodified().get(key);
    }
    static Snapshot current(){return snapshot;}

    private static JsonObject page(int page) throws Exception{
        var request=HttpRequest.newBuilder(URI.create("https://api.hypixel.net/v2/skyblock/auctions?page="+page))
            .timeout(Duration.ofSeconds(20)).header("User-Agent","Skyveil auction prices").GET().build();
        var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());
        var json=JsonParser.parseString(response.body()).getAsJsonObject();
        if(!json.has("success")||!json.get("success").getAsBoolean())
            throw new IllegalStateException("Unsuccessful auction response");
        return json;
    }

    static void refresh(){
        try{
            Snapshot previous=snapshot;
            Snapshot next=updatedSnapshot(AuctionPrices::page,previous);
            if(next==previous){
                nextAttempt=System.currentTimeMillis()+REFRESH_INTERVAL;
                return;
            }
            synchronized(HISTORY){
                HISTORY.update(next.generation(),next.similar(),System.currentTimeMillis());
            }
            // Publish only after the matching history update; readers must not see
            // a fresh variant map whose first observation has not been recorded.
            snapshot=next;lastFailure="none";
            nextAttempt=System.currentTimeMillis()+REFRESH_INTERVAL;
            LOGGER.info("Loaded active BIN prices and averages for {} item identities",next.prices().size());
        }catch(Exception error){
            lastFailure=(error.getCause()==null?error:error.getCause()).toString();
            nextAttempt=System.currentTimeMillis()+REFRESH_INTERVAL;
            LOGGER.warn("Could not refresh Auction House prices; retaining {} previous identities: {}",
                snapshot.prices().size(),error.toString());
        }finally{FETCHING.set(false);}
    }

    @FunctionalInterface interface PageSource {JsonObject get(int page) throws Exception;}

    static Snapshot updatedSnapshot(PageSource source,Snapshot previous) throws Exception{
        JsonObject first=source.get(0);
        if(first.get("lastUpdated").getAsLong()<=previous.generation())return previous;
        var initial=new AtomicBoolean(true);
        return download(index->index==0&&initial.getAndSet(false)?first:source.get(index));
    }

    /** Retry only rollover, never publish partial/mixed generations or retry HTTP failures in a loop. */
    static Snapshot download(PageSource source) throws Exception{
        for(int attempt=0;attempt<3;attempt++){
            try{return downloadGeneration(source);}
            catch(GenerationChanged changed){if(attempt==2)throw changed;}
        }
        throw new IllegalStateException("No auction snapshot");
    }

    private static Snapshot downloadGeneration(PageSource source) throws Exception{
        JsonObject first=source.get(0);
        int pages=first.get("totalPages").getAsInt();
        if(pages<1||pages>250)throw new IllegalStateException("Invalid auction page count");
        long generation=first.get("lastUpdated").getAsLong(),started=System.currentTimeMillis();
        Map<String,Double> next=new HashMap<>();
        Map<String,PriorityQueue<Double>> samples=new HashMap<>(),cleanSamples=new HashMap<>(),similarSamples=new HashMap<>();
        int eligible=0,decoded=0,malformed=0;
        var seen=new HashSet<String>();
        String firstError="";
        var executor=Executors.newFixedThreadPool(2,Thread.ofPlatform().daemon().priority(Thread.MIN_PRIORITY).name("Skyveil-Auction-Download-",0).factory());
        var downloads=new HashMap<Integer,Future<JsonObject>>();
        try{
            // Keep only two prefetched pages instead of retaining the entire auction JSON.
            for(int index=1;index<Math.min(pages,3);index++){
                final int requested=index;
                downloads.put(requested,executor.submit(()->source.get(requested)));
            }
            for(int p=0;p<pages;p++){
                long remaining=180_000-(System.currentTimeMillis()-started);
                if(remaining<=0)throw new TimeoutException("Auction download deadline exceeded");
                var current=p==0?first:downloads.remove(p).get(remaining,TimeUnit.MILLISECONDS);
                if(p==0)first=null;
                else if(p+2<pages){
                    final int requested=p+2;
                    downloads.put(requested,executor.submit(()->source.get(requested)));
                }
                if(current.get("lastUpdated").getAsLong()!=generation
                    ||current.get("totalPages").getAsInt()!=pages||current.get("page").getAsInt()!=p)
                    throw new GenerationChanged();
                for(var element:current.getAsJsonArray("auctions")){
                    try{
                        var auction=element.getAsJsonObject();
                        if(!eligible(auction,System.currentTimeMillis()))continue;
                        if(auction.has("uuid")&&!seen.add(auction.get("uuid").getAsString()))continue;
                        eligible++;
                        Listing listing=decode(auction);
                        decoded++;
                        if(listing!=null){
                            next.merge(listing.key(),listing.unitPrice(),Math::min);
                            sample(samples,listing.key(),listing.unitPrice());
                            if(!listing.similarity().isEmpty())sample(similarSamples,listing.similarity(),listing.unitPrice());
                            if(listing.unmodified())sample(cleanSamples,listing.key(),listing.unitPrice());
                        }
                    }catch(Exception invalid){
                        malformed++;
                        if(firstError.isEmpty())firstError=invalid.toString();
                    }
                }
            }
        }finally{
            for(var future:downloads.values())future.cancel(true);
            executor.shutdownNow();
        }
        if(malformed>0)LOGGER.warn("Skipped {} malformed listings out of {} active BIN listings: {}",malformed,eligible,firstError);
        // A mostly undecodable response must not replace a healthy full-market snapshot.
        if(next.isEmpty()||decoded*2<eligible)throw new IllegalStateException("Incomplete auction item decoding");
        return new Snapshot(Map.copyOf(next),System.currentTimeMillis(),means(cleanSamples),means(samples),generation,means(similarSamples));
    }

    /** At most five cheapest offers of the same item identity, excluding expensive outliers. */
    private static void sample(Map<String,PriorityQueue<Double>> samples,String key,double price){
        var values=samples.computeIfAbsent(key,ignored->new PriorityQueue<>(Comparator.reverseOrder()));
        values.add(price);if(values.size()>5)values.poll();
    }
    private static Map<String,Double> means(Map<String,PriorityQueue<Double>> samples){
        var result=new HashMap<String,Double>();
        samples.forEach((key,values)->result.put(key,values.stream().mapToDouble(Double::doubleValue).average().orElseThrow()));
        return Map.copyOf(result);
    }

    static Listing decode(JsonObject auction) throws Exception{
        var encoded=auction.get("item_bytes");
        String bytes=encoded.isJsonObject()?encoded.getAsJsonObject().get("data").getAsString():encoded.getAsString();
        if(bytes.length()>1_000_000)throw new IllegalArgumentException("Oversized auction item");
        var root=NbtIo.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(bytes)),NbtAccounter.create(2_000_000));
        var item=root.getListOrEmpty("i").getCompoundOrEmpty(0);
        var extra=BazaarTooltip.attributes(item);
        if(extra.getStringOr("id","").isBlank())throw new IllegalArgumentException("Missing SkyBlock item ID");
        String key=identity(extra);
        if(key.isBlank())return null;
        int count=item.getIntOr("Count",item.getIntOr("count",1));
        if(count<1)throw new IllegalArgumentException("Invalid auction stack count");
        double unit=auction.get("starting_bid").getAsDouble()/count;
        if(!Double.isFinite(unit)||unit<=0)throw new IllegalArgumentException("Invalid BIN price");
        return new Listing(key,unit,isUnmodified(extra),AuctionSimilarity.key(extra));
    }

    static boolean eligible(JsonObject auction,long now){
        return auction.has("bin")&&auction.get("bin").getAsBoolean()
            &&(!auction.has("claimed")||!auction.get("claimed").getAsBoolean())
            &&auction.get("end").getAsLong()>now
            &&(!auction.has("highest_bid_amount")||auction.get("highest_bid_amount").getAsDouble()==0)
            &&(!auction.has("bids")||auction.getAsJsonArray("bids").isEmpty());
    }

    /**
     * Builds a market identity, not an instance ID. Pets include calculated level;
     * supported applied upgrades are handled separately by AuctionSimilarity.
     */
    public static String identity(CompoundTag extra){
        String id=extra.getStringOr("id","").trim().toUpperCase(Locale.ROOT);
        if(id.equals("PET"))try{
            String info=extra.getStringOr("petInfo","");
            com.google.gson.JsonElement value;
            try{value=JsonParser.parseString(info);}
            catch(com.google.gson.JsonParseException invalid){value=JsonParser.parseString(info.replace("\\\"","\""));}
            if(value.isJsonPrimitive())value=JsonParser.parseString(value.getAsString());
            var pet=value.getAsJsonObject();
            String type=pet.get("type").getAsString().trim().toUpperCase(Locale.ROOT);
            String tier=pet.get("tier").getAsString().trim().toUpperCase(Locale.ROOT);
            double xp=pet.has("exp")?pet.get("exp").getAsDouble():0;
            if(type.isEmpty()||tier.isEmpty()||!Double.isFinite(xp)||xp<0)return "";
            int level=name.skyveil.client.pet.PetXpCalculator.levelFromTotalXp(xp,type,
                name.skyveil.client.itemrarity.SkyblockRarity.valueOf(tier));
            return level<1?"":"PET:"+type+":"+tier+":LVL:"+level;
        }catch(RuntimeException ignored){return "";}
        // Variant items must never share a price merely because their base IDs match.
        if(id.equals("RUNE")){
            var runes=extra.getCompoundOrEmpty("runes");
            if(runes.keySet().size()!=1)return "";
            String rune=runes.keySet().iterator().next();
            int level=runes.getIntOr(rune,0);
            return level>0?"RUNE:"+rune.toUpperCase(Locale.ROOT)+":"+level:"";
        }
        if(id.equals("ENCHANTED_BOOK")){
            var enchantments=extra.getCompoundOrEmpty("enchantments");
            if(enchantments.isEmpty())return "";
            var parts=new TreeMap<String,Integer>();
            for(String key:enchantments.keySet()){
                int level=enchantments.getIntOr(key,0);
                if(level<=0)return "";
                parts.put(key.toUpperCase(Locale.ROOT),level);
            }
            return "BOOK:"+parts.entrySet().stream().map(entry->entry.getKey()+"="+entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
        }
        if(id.equals("POTION")||id.equals("ATTRIBUTE_SHARD"))return "";
        return id;
    }

    public static boolean hasOnlyInnateKuudraAttributes(CompoundTag extra){
        String id=extra.getStringOr("id","");
        if(!id.matches("(?:HOT_|BURNING_|FIERY_|INFERNAL_)?(AURORA|CRIMSON|TERROR|FERVOR|HOLLOW)_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"))return false;
        var attributes=extra.getCompoundOrEmpty("attributes");
        return attributes.keySet().size()<=2&&attributes.keySet().stream().allMatch(key->attributes.getIntOr(key,0)==1);
    }
    public static boolean isUnmodified(CompoundTag extra){
        if(!extra.getStringOr("id","").equals("RUNE")&&!extra.getCompoundOrEmpty("runes").isEmpty())return false;
        for(String key:List.of("modifier","gems","enchantments","attributes","ability_scroll","power_ability_scroll",
            "rarity_upgrades","hot_potato_count","upgrade_level","dungeon_item_level","art_of_war_count",
            "art_of_peace_count","artOfPeaceApplied","stats_book","farming_for_dummies_count","wood_singularity_count","tuned_transmission",
            "mana_disintegrator_count","jalapeno_count","polarvoid","book_of_stats","ethermerge",
            "drill_part_engine","drill_part_fuel_tank","drill_part_upgrade_module","dye_item","skin")){
            if(key.equals("enchantments")){
                if(extra.getStringOr("id","").equals("ENCHANTED_BOOK"))continue;
                if(name.skyveil.client.craftcost.CraftCostTooltip.paidEnchantments(extra).isEmpty())continue;
                return false;
            }
            if(key.equals("attributes")&&hasOnlyInnateKuudraAttributes(extra))continue;
            var value=extra.get(key);if(value==null)continue;
            if(value.asCompound().isPresent()){if(!value.asCompound().get().isEmpty())return false;}
            else if(value.asList().isPresent()){if(!value.asList().get().isEmpty())return false;}
            else if(value.asInt().isPresent()){if(value.asInt().get()!=0)return false;}
            else if(!value.asString().orElse("").isEmpty())return false;
        }
        return true;
    }
    record Snapshot(Map<String,Double> prices,long received,Map<String,Double> unmodified,
                    Map<String,Double> averages,long generation,Map<String,Double> similar){
        Snapshot(Map<String,Double> prices,long received){this(prices,received,Map.of(),Map.of(),0,Map.of());}
    }
    record Listing(String key,double unitPrice,boolean unmodified,String similarity){
        Listing(String key,double unitPrice){this(key,unitPrice,true,plainSimilarity(key));}
        private static String plainSimilarity(String key){
            var extra=new CompoundTag();extra.putString("id",key);return AuctionSimilarity.key(extra);
        }
    }
    private static final class GenerationChanged extends Exception{
        private static final long serialVersionUID=1L;
        GenerationChanged(){super("Auction generation changed during download");}
    }
}