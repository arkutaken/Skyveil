package name.skyveil.client.bazaar;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.HashMap;

/** Full-market, decimal quotes from the existing shared Bazaar request. */
public final class BazaarPrices {
    // Publish quotes and timestamps together so render readers never mix generations.
    private record Snapshot(Map<String,Quote> prices,long received,long apiUpdated){}
    private static volatile Snapshot snapshot=new Snapshot(Map.of(),0,0);
    private BazaarPrices(){}
    public static long revision(){return snapshot.received();}
    public record Quote(Double buy,Double sell){}
    // The shard service supplies the same response to this full-market view;
    // unchanged API generations must not advance its revision or freshness time.
    public static synchronized void update(JsonObject root){
        if(!root.has("success")||!root.get("success").getAsBoolean())return;
        long updated=root.has("lastUpdated")?root.get("lastUpdated").getAsLong():0;
        if(updated>0&&updated<=snapshot.apiUpdated())return;
        snapshot=new Snapshot(parse(root),System.currentTimeMillis(),updated);
    }
    public static boolean hasSnapshot(){return !snapshot.prices().isEmpty();}
    public static boolean isProduct(String id){return snapshot.prices().containsKey(id);}
    public static Quote quote(String product){
        name.skyveil.client.hunting.ShardPriceService.ensureFresh();
        Snapshot current=snapshot;
        return System.currentTimeMillis()-current.received()>180_000?null:current.prices().get(product);
    }
    public static Map<String,Quote> parse(JsonObject root){
        Map<String,Quote> result=new HashMap<>();
        if(!root.has("products")||!root.get("products").isJsonObject())return Map.of();
        for(var entry:root.getAsJsonObject("products").entrySet()){
            if(!entry.getValue().isJsonObject())continue;
            var product=entry.getValue().getAsJsonObject();
            result.put(entry.getKey(),new Quote(best(product,"buy_summary",true),best(product,"sell_summary",false)));
        }
        return Map.copyOf(result);
    }
    private static Double best(JsonObject product,String side,boolean lowest){
        if(!product.has(side)||!product.get(side).isJsonArray())return null;
        Double best=null;
        for(var order:product.getAsJsonArray(side))try{
            var obj=order.getAsJsonObject();double price=obj.get("pricePerUnit").getAsDouble();
            if(obj.get("amount").getAsDouble()<=0||!Double.isFinite(price)||price<=0)continue;
            if(best==null||(lowest?price<best:price>best))best=price;
        }catch(RuntimeException ignored){}
        return best;
    }
}