package name.skyveil.client.bazaar;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.HashMap;

/** Full-market, decimal quotes from the existing shared Bazaar request. */
public final class BazaarPrices {
    private static volatile Map<String,Quote> prices=Map.of();
    private static volatile long received,apiUpdated;
    private BazaarPrices(){}
    public static long revision(){return received;}
    public record Quote(Double buy,Double sell){}
    public static void update(JsonObject root){
        if(!root.has("success")||!root.get("success").getAsBoolean())return;
        long updated=root.has("lastUpdated")?root.get("lastUpdated").getAsLong():0;
        if(updated>0&&updated<=apiUpdated)return;
        prices=parse(root);received=System.currentTimeMillis();apiUpdated=updated;
    }
    public static boolean hasSnapshot(){return !prices.isEmpty();}
    public static boolean isProduct(String id){return prices.containsKey(id);}
    public static Quote quote(String product){
        name.skyveil.client.hunting.ShardPriceService.ensureFresh();
        return System.currentTimeMillis()-received>180_000?null:prices.get(product);
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