package name.skyveil.client.hunting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shard-only Bazaar snapshot service using Hypixel's public, keyless Bazaar endpoint. */
public final class ShardPriceService {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-shard-prices");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final URI ENDPOINT=URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
    private static final long SUCCESS_REFRESH_MS=5*60_000L,FAILURE_RETRY_MS=60_000L;
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final AtomicBoolean INITIALIZED=new AtomicBoolean(),REFRESHING=new AtomicBoolean();
    private static volatile Snapshot snapshot=new Snapshot(Map.of(),0,0,false);
    private static volatile long lastAttempt;
    private ShardPriceService(){}

    public static void initialize(){
        if(!INITIALIZED.compareAndSet(false,true))return;
        Thread.startVirtualThread(()->{loadCache();refreshAsync();});
    }

    public static void ensureFresh(){
        if(!INITIALIZED.get())initialize();
        Snapshot current=snapshot;long interval=current.fromNetwork()?SUCCESS_REFRESH_MS:FAILURE_RETRY_MS;
        if(System.currentTimeMillis()-lastAttempt>=interval)refreshAsync();
    }

    public static Quote quote(String bazaarProduct){ensureFresh();return bazaarProduct==null?null:snapshot.prices().get(bazaarProduct);}
    public static Status status(){
        Snapshot current=snapshot;if(REFRESHING.get()&&current.prices().isEmpty())return Status.LOADING;
        if(current.prices().isEmpty())return Status.UNAVAILABLE;
        return current.fromNetwork()?Status.LIVE:Status.CACHED;
    }
    public static int pricedProducts(){return snapshot.prices().size();}
    public static String format(long coins){
        double value=coins;String suffix="";
        if(Math.abs(value)>=1_000_000_000_000L){value/=1_000_000_000_000D;suffix="t";}
        else if(Math.abs(value)>=1_000_000_000L){value/=1_000_000_000D;suffix="b";}
        else if(Math.abs(value)>=1_000_000L){value/=1_000_000D;suffix="m";}
        else if(Math.abs(value)>=1_000L){value/=1_000D;suffix="k";}
        if(suffix.isEmpty())return String.format(Locale.ROOT,"%,d",coins);
        return String.format(Locale.ROOT,value>=100?"%.0f%s":value>=10?"%.1f%s":"%.2f%s",value,suffix);
    }

    static Map<String,Quote> parseProducts(JsonObject root){
        if(root==null||!booleanValue(root,"success")||!root.has("products")||!root.get("products").isJsonObject())return Map.of();
        HashMap<String,Quote> prices=new HashMap<>();
        for(var entry:root.getAsJsonObject("products").entrySet()){
            if(!entry.getKey().startsWith("SHARD_")||!entry.getValue().isJsonObject())continue;
            JsonObject product=entry.getValue().getAsJsonObject();if(!product.has("quick_status")||!product.get("quick_status").isJsonObject())continue;
            JsonObject quick=product.getAsJsonObject("quick_status");Long instantBuy=positiveRounded(quick.get("buyPrice")),instantSell=positiveRounded(quick.get("sellPrice"));
            if(instantBuy!=null||instantSell!=null)prices.put(entry.getKey(),new Quote(instantBuy,instantSell));
        }
        return Map.copyOf(prices);
    }

    private static void refreshAsync(){
        if(!REFRESHING.compareAndSet(false,true))return;lastAttempt=System.currentTimeMillis();
        Thread.startVirtualThread(()->{try{
            HttpRequest request=HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(20)).header("Accept","application/json").header("User-Agent","Skyveil/1.10.1 shard-price-service").GET().build();
            HttpResponse<String> response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());
            JsonObject root=JsonParser.parseString(response.body()).getAsJsonObject();Map<String,Quote> prices=parseProducts(root);if(prices.isEmpty())throw new IllegalStateException("response contained no shard prices");
            long fetchedAt=System.currentTimeMillis(),apiUpdated=longValue(root,"lastUpdated");snapshot=new Snapshot(prices,fetchedAt,apiUpdated,true);writeCache(new CacheFile(fetchedAt,apiUpdated,prices));
            LOGGER.info("Loaded {} shard prices from the Hypixel Bazaar snapshot",prices.size());
        }catch(Exception exception){LOGGER.warn("Could not refresh Hypixel Bazaar shard prices; retaining the last known snapshot",exception);}
        finally{REFRESHING.set(false);}});
    }

    private static void loadCache(){
        Path path=cachePath();if(!Files.isRegularFile(path))return;
        try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){
            CacheFile cached=GSON.fromJson(reader,CacheFile.class);if(cached!=null&&cached.prices()!=null&&!cached.prices().isEmpty())snapshot=new Snapshot(Map.copyOf(cached.prices()),cached.fetchedAt(),cached.apiLastUpdated(),false);
        }catch(Exception exception){LOGGER.warn("Could not read cached shard prices from {}",path,exception);}
    }
    private static void writeCache(CacheFile cache){
        Path path=cachePath(),temporary=path.resolveSibling("shards.json.tmp");
        try{Files.createDirectories(path.getParent());try(Writer writer=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)){GSON.toJson(cache,writer);}try{Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception unsupported){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}}
        catch(Exception exception){LOGGER.warn("Could not persist shard prices to {}",path,exception);}
    }
    private static Path cachePath(){return FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("cache").resolve("shards.json");}
    private static boolean booleanValue(JsonObject object,String key){try{return object.get(key).getAsBoolean();}catch(Exception ignored){return false;}}
    private static long longValue(JsonObject object,String key){try{return object.get(key).getAsLong();}catch(Exception ignored){return 0;}}
    private static Long positiveRounded(JsonElement element){try{double value=element.getAsDouble();return Double.isFinite(value)&&value>0&&value<Long.MAX_VALUE?Math.round(value):null;}catch(Exception ignored){return null;}}

    public enum Status{LOADING,LIVE,CACHED,UNAVAILABLE}
    public record Quote(Long instantBuy,Long instantSell){}
    private record Snapshot(Map<String,Quote> prices,long fetchedAt,long apiLastUpdated,boolean fromNetwork){}
    private record CacheFile(long fetchedAt,long apiLastUpdated,Map<String,Quote> prices){}
}
