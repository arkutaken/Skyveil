package name.skyveil.client.update;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Shows bundled release notes once after the installed Skyveil version changes. */
public final class ReleaseNoticeManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-release-notice");
    private static final String VERSION=FabricLoader.getInstance().getModContainer("skyveil")
        .map(container->container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    private static final List<String> NOTES=loadNotes();
    private static boolean pending;
    private ReleaseNoticeManager(){}

    public static void initialize(){pending=!VERSION.equals(readLastSeenVersion());}

    public static void tick(Minecraft client){
        if(!pending||client.player==null)return;
        show(client);pending=false;writeLastSeenVersion();
    }

    public static boolean showNow(Minecraft client){if(client.player==null)return false;show(client);pending=false;writeLastSeenVersion();return true;}

    static List<String> parseNotes(String text){
        if(text==null||text.isBlank())return List.of();
        return text.lines().map(String::trim).filter(line->!line.isEmpty()).toList();
    }

    private static void show(Minecraft client){
        client.player.sendSystemMessage(Component.literal("[Skyveil] ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal("Version "+VERSION+" installed").withStyle(ChatFormatting.GOLD)));
        if(NOTES.isEmpty())return;
        client.player.sendSystemMessage(Component.literal("What's new:").withStyle(ChatFormatting.YELLOW));
        for(String note:NOTES)client.player.sendSystemMessage(Component.literal(" • "+note).withStyle(ChatFormatting.WHITE));
    }

    private static List<String> loadNotes(){
        try(InputStream stream=ReleaseNoticeManager.class.getResourceAsStream("/assets/skyveil/release_notes.txt")){
            if(stream==null){LOGGER.warn("Bundled release notes are missing");return List.of();}
            return parseNotes(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
        }catch(Exception exception){LOGGER.warn("Could not read bundled release notes",exception);return List.of();}
    }

    private static String readLastSeenVersion(){
        Path path=statePath();
        try{return Files.isRegularFile(path)?Files.readString(path,StandardCharsets.UTF_8).trim():"";}
        catch(Exception exception){LOGGER.warn("Could not read release-notice state from {}",path,exception);return "";}
    }

    private static void writeLastSeenVersion(){
        Path path=statePath(),temporary=path.resolveSibling("last-seen-version.txt.tmp");
        try{
            Files.createDirectories(path.getParent());Files.writeString(temporary,VERSION,StandardCharsets.UTF_8);
            try{Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(Exception unsupported){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception exception){LOGGER.warn("Could not persist release-notice state to {}",path,exception);}
    }

    private static Path statePath(){return FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("last-seen-version.txt");}
}
