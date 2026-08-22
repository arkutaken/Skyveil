package name.skyveil.client.map;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves generated, bundled map textures without runtime downloads or world parsing. */
public final class MapTextureManager {
    private static final Map<String,Identifier> READY=new ConcurrentHashMap<>();
    private static final Map<String,String> STATUS=new ConcurrentHashMap<>();
    private static final Set<String> CHECKED=ConcurrentHashMap.newKeySet();
    private MapTextureManager() {}

    /** Resolves every immutable bundled identifier during client startup, before HUD rendering. */
    public static void initialize(){for(SkyblockMapDefinition map:SkyblockMapRegistry.all())resolve(map);}

    public static Identifier texture(SkyblockMapDefinition map){
        Identifier ready=READY.get(map.id);if(ready!=null)return ready;
        resolve(map);return READY.get(map.id);
    }

    private static void resolve(SkyblockMapDefinition map){
        if(!CHECKED.add(map.id))return;
        try{
            String raw=map.texture==null?"":map.texture.trim();int split=raw.indexOf(':');
            if(split<1||split==raw.length()-1)throw new IllegalArgumentException("Invalid bundled texture identifier");
            Identifier id=Identifier.fromNamespaceAndPath(raw.substring(0,split),raw.substring(split+1));
            READY.put(map.id,id);STATUS.put(map.id,"Ready");
        }catch(Exception error){STATUS.put(map.id,"Map asset unavailable");}
    }
    public static String status(SkyblockMapDefinition map){return STATUS.getOrDefault(map.id,"Map asset unavailable");}
}
