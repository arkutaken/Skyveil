package name.skyveil.client.cache;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Validated atomic storage for the one replace-in-place Skyveil runtime cache. */
final class SkyveilCacheFile {
    static final int SCHEMA=1;
    static final long MAX_FILE_BYTES=32L*1024L*1024L;
    static final int MAX_MEMORY_BYTES=48*1024*1024;

    private SkyveilCacheFile(){}

    static LoadResult load(Path path){
        if(path==null||!Files.isRegularFile(path))return new LoadResult(emptyRoot(),Status.MISSING);
        try{
            long size=Files.size(path);if(size<=0||size>MAX_FILE_BYTES)return new LoadResult(emptyRoot(),Status.REJECTED);
            CompoundTag root=NbtIo.read(path);if(!valid(root))return new LoadResult(emptyRoot(),Status.REJECTED);
            return new LoadResult(root,Status.VALID);
        }catch(Exception exception){return new LoadResult(emptyRoot(),Status.REJECTED);}
    }

    // Write and validate the temporary file before replacing the old cache. The
    // finally block removes the temporary path after success or failure.
    static boolean save(Path path,CompoundTag root)throws IOException{
        if(path==null||!valid(root))return false;
        Files.createDirectories(path.getParent());Path temporary=temporary(path);
        try{
            NbtIo.write(root,temporary);
            if(Files.size(temporary)>MAX_FILE_BYTES)throw new IOException("Skyveil cache exceeds "+MAX_FILE_BYTES+" bytes");
            try{Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(Exception unsupported){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
            return true;
        }finally{Files.deleteIfExists(temporary);}
    }

    static CompoundTag emptyRoot(){CompoundTag root=new CompoundTag();root.putInt("schema",SCHEMA);root.put("global",new CompoundTag());root.put("profiles",new CompoundTag());return root;}
    static Path temporary(Path path){return path.resolveSibling(path.getFileName()+".tmp");}
    private static boolean valid(CompoundTag root){if(root==null||root.getIntOr("schema",0)!=SCHEMA||root.sizeInBytes()>MAX_MEMORY_BYTES)return false;CompoundTag profiles=root.getCompoundOrEmpty("profiles");if(profiles.size()>16)return false;for(String account:profiles.keySet()){if(account.length()>64||profiles.getCompound(account).isEmpty()||profiles.getCompoundOrEmpty(account).sizeInBytes()>12*1024*1024)return false;}return root.getCompound("global").isPresent();}

    enum Status{MISSING,VALID,REJECTED}
    record LoadResult(CompoundTag root,Status status){}
}
