package name.skyveil.client.cache;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Owns Skyveil's single persisted runtime cache. Features update memory only;
 * the Fabric client-stopping event performs the sole atomic disk replacement.
 */
public final class SkyveilCacheManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-cache");
    private static final Object LOCK=new Object();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("skyveil-cache.nbt");
    private static final Set<Path> OBSOLETE=new HashSet<>();
    private static volatile boolean loaded;
    private static CompoundTag root=SkyveilCacheFile.emptyRoot();
    private static boolean dirty;
    private static CompletableFuture<Void> loading;

    private SkyveilCacheManager(){}

    public static void initialize(){
        synchronized(LOCK){if(loading!=null)return;loading=CompletableFuture.runAsync(SkyveilCacheManager::loadAndMigrate,command->Thread.startVirtualThread(command));}
    }

    public static boolean isLoaded(){return loaded;}
    public static void awaitLoaded(){CompletableFuture<Void> future; synchronized(LOCK){future=loading;}if(future!=null)future.join();}

    public static CompoundTag profileSection(String account,String section){
        if(!loaded||!validKey(account,64)||!validKey(section,40))return null;
        synchronized(LOCK){CompoundTag profile=root.getCompoundOrEmpty("profiles").getCompoundOrEmpty(account);return profile.getCompound(section).map(CompoundTag::copy).orElse(null);}
    }

    public static void putProfileSection(String account,String section,CompoundTag value){
        if(!loaded||!validKey(account,64)||!validKey(section,40)||value==null||value.sizeInBytes()>12*1024*1024)return;
        synchronized(LOCK){CompoundTag profiles=root.getCompoundOrEmpty("profiles");if(!profiles.contains(account)&&profiles.size()>=16)return;CompoundTag profile=profiles.getCompoundOrEmpty(account),before=profile.getCompound(section).orElse(null);if(value.equals(before))return;CompoundTag candidate=root.copy(),candidateProfiles=candidate.getCompoundOrEmpty("profiles"),candidateProfile=candidateProfiles.getCompoundOrEmpty(account);candidateProfile.put(section,value.copy());candidateProfiles.put(account,candidateProfile);candidate.put("profiles",candidateProfiles);if(candidate.sizeInBytes()>SkyveilCacheFile.MAX_MEMORY_BYTES)return;root=candidate;dirty=true;}
    }

    public static CompoundTag globalSection(String section){if(!loaded||!validKey(section,40))return null;synchronized(LOCK){return root.getCompoundOrEmpty("global").getCompound(section).map(CompoundTag::copy).orElse(null);}}
    public static void putGlobalSection(String section,CompoundTag value){if(!loaded||!validKey(section,40)||value==null||value.sizeInBytes()>8*1024*1024)return;synchronized(LOCK){CompoundTag global=root.getCompoundOrEmpty("global");CompoundTag before=global.getCompound(section).orElse(null);if(value.equals(before))return;CompoundTag candidate=root.copy(),candidateGlobal=candidate.getCompoundOrEmpty("global");candidateGlobal.put(section,value.copy());candidate.put("global",candidateGlobal);if(candidate.sizeInBytes()>SkyveilCacheFile.MAX_MEMORY_BYTES)return;root=candidate;dirty=true;}}
    public static void registerObsolete(Path path){if(path==null)return;synchronized(LOCK){OBSOLETE.add(path.toAbsolutePath().normalize());}}

    public static void shutdown(){
        awaitLoaded();CompoundTag snapshot;boolean shouldSave;long started=System.nanoTime();synchronized(LOCK){snapshot=root.copy();shouldSave=dirty;}
        boolean saved=!shouldSave;try{if(shouldSave)saved=SkyveilCacheFile.save(PATH,snapshot);}catch(Exception exception){LOGGER.error("Could not save {}",PATH,exception);}
        if(saved){synchronized(LOCK){dirty=false;}cleanupObsolete();}
        LOGGER.info("Skyveil cache shutdown: saved={} dirty={} bytes={} durationMs={}",saved,shouldSave,snapshot.sizeInBytes(),(System.nanoTime()-started)/1_000_000L);
    }

    public static Path path(){return PATH;}

    private static void loadAndMigrate(){
        Path skyveilDirectory=FabricLoader.getInstance().getConfigDir().resolve("skyveil");SkyveilCacheFile.LoadResult result=SkyveilCacheFile.load(PATH);CompoundTag loadedRoot=result.root();boolean changed=result.status()==SkyveilCacheFile.Status.REJECTED;
        changed|=migrateLegacyNbtDirectory(loadedRoot,"equipment",skyveilDirectory.resolve("equipment_showcase"));
        changed|=migrateLegacyNbtDirectory(loadedRoot,"storage",skyveilDirectory.resolve("storage_preview"));
        // This directory belonged exclusively to the removed shards.json cache.
        registerObsolete(skyveilDirectory.resolve("cache"));
        synchronized(LOCK){root=loadedRoot;dirty=changed;loaded=true;}
        LOGGER.info("Skyveil cache loaded: status={} profiles={} bytes={}",result.status(),loadedRoot.getCompoundOrEmpty("profiles").size(),loadedRoot.sizeInBytes());
    }

    private static boolean migrateLegacyNbtDirectory(CompoundTag target,String section,Path directory){
        if(!Files.isDirectory(directory))return false;boolean changed=false;try(var files=Files.list(directory)){
            for(Path file:files.toList()){registerObsolete(file);if(!file.getFileName().toString().endsWith(".nbt")||Files.size(file)>SkyveilCacheFile.MAX_FILE_BYTES)continue;CompoundTag legacy;try{legacy=NbtIo.read(file);}catch(Exception ignored){continue;}if(legacy==null||legacy.getIntOr("schema",0)!=1)continue;String name=file.getFileName().toString(),account=name.substring(0,name.length()-4);if(!validKey(account,64))continue;CompoundTag profiles=target.getCompoundOrEmpty("profiles"),profile=profiles.getCompoundOrEmpty(account);if(profile.contains(section))continue;profile.put(section,legacy.copy());profiles.put(account,profile);target.put("profiles",profiles);changed=true;}
        }catch(Exception exception){LOGGER.warn("Could not migrate legacy {} cache directory {}",section,directory,exception);}registerObsolete(directory);return changed;
    }

    private static void cleanupObsolete(){ArrayList<Path> paths; synchronized(LOCK){paths=new ArrayList<>(OBSOLETE);OBSOLETE.clear();}paths.sort((left,right)->Integer.compare(right.getNameCount(),left.getNameCount()));for(Path path:paths)try{if(Files.isDirectory(path)){try(var children=Files.list(path)){if(children.findAny().isEmpty())Files.deleteIfExists(path);}}else Files.deleteIfExists(path);}catch(Exception exception){LOGGER.debug("Could not remove obsolete cache path {}",path,exception);}}
    private static boolean validKey(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max&&value.chars().allMatch(character->Character.isLetterOrDigit(character)||character=='-'||character=='_'||character==':');}
}
