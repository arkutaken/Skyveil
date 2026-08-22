package name.skyveil.client.hunting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Durable, account-scoped snapshots of observed Attribute Menu progress. */
final class AttributeProgressStore {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-attribute-cache");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("attribute_progress.json");
    private static final int SCHEMA=1;
    private AttributeProgressStore(){}

    static synchronized Snapshot load(String account){
        Root root=read();Profile profile=root.profiles.get(account);
        if(profile==null||profile.catalogSize!=AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES)return Snapshot.empty();
        return new Snapshot(Math.max(1,profile.totalPages),List.copyOf(profile.visitedPages),Map.copyOf(profile.owned),List.copyOf(profile.rows));
    }
    static synchronized void save(String account,Snapshot snapshot){
        if(account==null||account.isBlank())return;
        Root root=read();Profile profile=new Profile();profile.catalogSize=AttributeSessionData.TOTAL_CONSUMABLE_ATTRIBUTES;profile.totalPages=Math.max(1,snapshot.totalPages());profile.visitedPages=new ArrayList<>(snapshot.visitedPages());profile.owned=new HashMap<>(snapshot.owned());profile.rows=new ArrayList<>(snapshot.rows());root.profiles.put(account,profile);
        try{
            Files.createDirectories(PATH.getParent());Path temporary=PATH.resolveSibling("attribute_progress.json.tmp");
            try(Writer writer=Files.newBufferedWriter(temporary)){GSON.toJson(root,writer);}
            try{Files.move(temporary,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception unsupported){Files.move(temporary,PATH,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception exception){LOGGER.error("Could not save {}",PATH,exception);}
    }
    private static Root read(){
        if(!Files.exists(PATH))return new Root();
        try(Reader reader=Files.newBufferedReader(PATH)){Root root=GSON.fromJson(reader,Root.class);if(root!=null&&root.schema==SCHEMA&&root.profiles!=null)return root;}
        catch(Exception exception){LOGGER.warn("Could not load {}",PATH,exception);}
        return new Root();
    }
    record Snapshot(int totalPages,List<Integer> visitedPages,Map<String,Long> owned,List<SavedRow> rows){static Snapshot empty(){return new Snapshot(1,List.of(),Map.of(),List.of());}}
    record SavedRow(String subtype,int tier,int toNext,AttributeMenuParser.Ownership ownership,String sourceName){}
    private static final class Root{int schema=SCHEMA;Map<String,Profile> profiles=new HashMap<>();}
    private static final class Profile{int catalogSize;int totalPages=1;List<Integer> visitedPages=new ArrayList<>();Map<String,Long> owned=new HashMap<>();List<SavedRow> rows=new ArrayList<>();}
}
