package name.skyveil.client.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loads once and writes only after an explicit add/edit/delete action. */
public final class MapMarkerManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-map-markers");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE=new TypeToken<List<CustomMapMarker>>(){}.getType();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("map_markers.json");
    private static List<CustomMapMarker> markers=new ArrayList<>();
    private MapMarkerManager() {}

    public static void load(){
        if(!Files.exists(PATH))return;
        try(Reader reader=Files.newBufferedReader(PATH)){
            List<CustomMapMarker> loaded=GSON.fromJson(reader,TYPE);
            markers=new ArrayList<>();
            Set<String> ids=new HashSet<>();
            if(loaded!=null)for(CustomMapMarker marker:loaded)if(valid(marker)&&ids.add(marker.id()))markers.add(marker);
        }catch(Exception exception){LOGGER.warn("Could not load {}",PATH,exception);markers=new ArrayList<>();}
    }
    public static List<CustomMapMarker> forMap(String mapId){return markers.stream().filter(marker->marker.mapId().equals(mapId)).toList();}
    public static CustomMapMarker add(String mapId,String name,double x,double y,double z){
        CustomMapMarker marker=new CustomMapMarker(UUID.randomUUID().toString(),mapId,name.trim(),x,y,z);markers.add(marker);save();return marker;
    }
    public static void update(CustomMapMarker marker,String name,double x,double y,double z){
        for(int index=0;index<markers.size();index++)if(markers.get(index).id().equals(marker.id())){markers.set(index,new CustomMapMarker(marker.id(),marker.mapId(),name.trim(),x,y,z));save();return;}
    }
    public static void remove(CustomMapMarker marker){if(markers.removeIf(candidate->candidate.id().equals(marker.id())))save();}
    private static boolean valid(CustomMapMarker marker){
        return marker!=null&&marker.id()!=null&&!marker.id().isBlank()&&marker.mapId()!=null&&!marker.mapId().isBlank()
            &&marker.name()!=null&&!marker.name().isBlank()&&Double.isFinite(marker.x())&&Double.isFinite(marker.y())&&Double.isFinite(marker.z());
    }
    private static void save(){
        try{
            Files.createDirectories(PATH.getParent());Path temporary=PATH.resolveSibling("map_markers.json.tmp");
            try(Writer writer=Files.newBufferedWriter(temporary)){GSON.toJson(markers,TYPE,writer);}
            try{Files.move(temporary,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(Exception unsupported){Files.move(temporary,PATH,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception exception){LOGGER.error("Could not save {}",PATH,exception);}
    }
}
