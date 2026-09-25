package name.skyveil.client.hunting;

import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Bridges Hunting Box virtual IDs (for example U24) to real Bazaar shard item identities. */
public final class HuntingShardPriceIdentity {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-hunting-shard-market");
    private static final Catalog CATALOG=load();
    private HuntingShardPriceIdentity(){}

    // Virtual Hunting Box IDs are not Bazaar product names. Translate via the
    // bundled identity bridge, with a catalog display-name fallback.
    public static MarketIdentity resolve(ItemStack stack){
        if(stack==null||stack.isEmpty())return null;
        MarketIdentity byId=CATALOG.byKey().get(normalize(AttributeShardResolver.resolveInternalId(stack)));
        return byId!=null?byId:CATALOG.byKey().get(normalize(stack.getHoverName().getString()));
    }

    static MarketIdentity resolve(String id,String displayName){MarketIdentity result=CATALOG.byKey().get(normalize(id));return result!=null?result:CATALOG.byKey().get(normalize(displayName));}
    static int size(){return CATALOG.identities();}

    private static Catalog load(){
        Properties properties=new Properties();
        try(var stream=HuntingShardPriceIdentity.class.getResourceAsStream("/assets/skyveil/data/attribute_shard_market.properties")){
            if(stream==null)throw new IllegalStateException("resource is missing");properties.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            HashMap<String,MarketIdentity> result=new HashMap<>();int identities=0;
            for(String shardId:properties.stringPropertyNames()){
                String[] fields=properties.getProperty(shardId,"").split("\\|",3);if(fields.length!=3)continue;
                MarketIdentity identity=new MarketIdentity(shardId,fields[0],fields[1],fields[2]);identities++;
                result.put(normalize(shardId),identity);result.putIfAbsent(normalize(fields[2]),identity);result.putIfAbsent(normalize(fields[1]),identity);
                String internal=fields[1];if(internal.startsWith("ATTRIBUTE_SHARD_")&&internal.endsWith(";1"))result.putIfAbsent(normalize(internal.substring(16,internal.length()-2)),identity);
            }
            return new Catalog(Map.copyOf(result),identities);
        }catch(Exception exception){LOGGER.error("Could not load the Hunting Box to Bazaar shard identity bridge",exception);return new Catalog(Map.of(),0);}
    }
    static MarketIdentity resolveAttribute(String subtype){return CATALOG.byKey().get(normalize(subtype));}
    private static String normalize(String value){return value==null?"":value.replaceAll("(?:\\u00c2)?\\u00a7.","").replaceAll("(?i)\\s+shard$","").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_+|_+$","");}

    public record MarketIdentity(String shardId,String bazaarName,String internalName,String displayName){}
    private record Catalog(Map<String,MarketIdentity> byKey,int identities){}
}
