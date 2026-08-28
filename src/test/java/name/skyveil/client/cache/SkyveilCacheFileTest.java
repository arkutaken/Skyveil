package name.skyveil.client.cache;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SkyveilCacheFileTest {
    @TempDir Path directory;

    @Test void missingCacheStartsEmpty(){var result=SkyveilCacheFile.load(directory.resolve("skyveil-cache.nbt"));assertEquals(SkyveilCacheFile.Status.MISSING,result.status());assertEquals(SkyveilCacheFile.SCHEMA,result.root().getIntOr("schema",0));}
    @Test void saveAtomicallyReplacesOneFile()throws Exception{Path path=directory.resolve("skyveil-cache.nbt");CompoundTag first=SkyveilCacheFile.emptyRoot();first.getCompoundOrEmpty("global").putInt("value",1);assertTrue(SkyveilCacheFile.save(path,first));CompoundTag second=SkyveilCacheFile.emptyRoot();second.getCompoundOrEmpty("global").putInt("value",2);assertTrue(SkyveilCacheFile.save(path,second));assertEquals(2,SkyveilCacheFile.load(path).root().getCompoundOrEmpty("global").getIntOr("value",0));assertFalse(Files.exists(SkyveilCacheFile.temporary(path)));try(var files=Files.list(directory)){assertEquals(1,files.count());}}
    @Test void corruptedAndOutdatedCachesAreRejected()throws Exception{Path path=directory.resolve("skyveil-cache.nbt");Files.writeString(path,"not nbt");assertEquals(SkyveilCacheFile.Status.REJECTED,SkyveilCacheFile.load(path).status());CompoundTag outdated=SkyveilCacheFile.emptyRoot();outdated.putInt("schema",999);NbtIo.write(outdated,path);assertEquals(SkyveilCacheFile.Status.REJECTED,SkyveilCacheFile.load(path).status());}
    @Test void oversizedProfileSetIsRejected()throws Exception{Path path=directory.resolve("skyveil-cache.nbt");CompoundTag root=SkyveilCacheFile.emptyRoot(),profiles=root.getCompoundOrEmpty("profiles");for(int index=0;index<17;index++)profiles.put("account_"+index,new CompoundTag());root.put("profiles",profiles);NbtIo.write(root,path);assertEquals(SkyveilCacheFile.Status.REJECTED,SkyveilCacheFile.load(path).status());}
}
