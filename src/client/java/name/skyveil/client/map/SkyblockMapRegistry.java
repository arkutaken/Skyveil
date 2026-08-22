package name.skyveil.client.map;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local registry; map JSON is parsed once and never fetched at runtime. */
public final class SkyblockMapRegistry {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-maps");
    private static final Gson GSON=new Gson();
    private static final List<SkyblockMapDefinition> MAPS=new ArrayList<>();
    private static boolean loaded;
    private SkyblockMapRegistry() {}

    public static synchronized void load(){
        if(loaded)return;
        loaded=true;
        for(String id:List.of("hub","gold_mine","deep_caverns","dwarven_mines","park","spiders_den","the_end","crimson_isle","farming_islands","dungeon_hub"))load(id);
    }
    private static void load(String id){
        String path="assets/skyveil/data/maps/"+id+".json";
        try(var stream=SkyblockMapRegistry.class.getClassLoader().getResourceAsStream(path)){
            if(stream==null){LOGGER.warn("Missing bundled map definition {}",path);return;}
            SkyblockMapDefinition definition=GSON.fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),SkyblockMapDefinition.class);
            if(definition!=null&&!definition.id.isBlank()&&definition.maxX>definition.minX&&definition.maxZ>definition.minZ)MAPS.add(definition);
            else LOGGER.warn("Ignored invalid map definition {}",path);
        }catch(Exception exception){LOGGER.error("Could not load map definition {}",path,exception);}
    }
    public static List<SkyblockMapDefinition> all(){load();return List.copyOf(MAPS);}
    public static SkyblockMapDefinition byId(String id){return all().stream().filter(map->map.id.equals(id)).findFirst().orElse(null);}
    public static SkyblockMapDefinition matchLocationText(String text){
        String normalized=normalize(text);
        for(SkyblockMapDefinition map:all())for(String alias:map.aliases){String candidate=normalize(alias);if(normalized.equals(candidate)||normalized.endsWith(" "+candidate))return map;}
        return null;
    }
    private static String normalize(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9:' -]"," ").replaceAll("\\s+"," ").trim();}
}
