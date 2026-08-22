package name.skyveil.client.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

/** Checks and installs stable Skyveil releases published by the project owner on GitHub. */
public final class GitHubUpdateManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-updater");
    private static final URI LATEST_RELEASE=URI.create("https://api.github.com/repos/arkutaken/Skyveil/releases/latest");
    private static final String RELEASE_PATH_PREFIX="/arkutaken/Skyveil/releases/download/";
    private static final long MAX_JAR_BYTES=50L*1024*1024;
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final AtomicBoolean CHECKING=new AtomicBoolean(),DOWNLOADING=new AtomicBoolean();
    private static final String CURRENT_VERSION=FabricLoader.getInstance().getModContainer("skyveil")
        .map(container->container.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");
    private static volatile ReleaseInfo latest;private static volatile boolean announcePending,installedForRestart;
    private GitHubUpdateManager(){}

    public static void initialize(){checkAsync(false);}
    public static void tick(Minecraft client){if(announcePending&&client.player!=null){announcePending=false;showAvailable(client,latest);}}

    /** Invoked by the clickable chat action and /sv update. */
    public static int requestUpdate(Minecraft client){
        if(client.player==null)return 0;
        if(installedForRestart){chat(client,"Update installed. Restart Minecraft to load it.",ChatFormatting.GREEN);return 1;}
        if(DOWNLOADING.get()){chat(client,"An update is already downloading…",ChatFormatting.YELLOW);return 1;}
        ReleaseInfo release=latest;announcePending=false;
        if(release!=null&&compareVersions(release.version(),CURRENT_VERSION)>0){downloadAsync(client,release);return 1;}
        chat(client,CHECKING.get()?"Checking GitHub for updates…":"Checking GitHub for updates…",ChatFormatting.YELLOW);checkAsync(true);return 1;
    }

    static ReleaseInfo parseRelease(JsonObject root){
        if(root==null||booleanValue(root,"draft")||booleanValue(root,"prerelease"))throw new IllegalArgumentException("latest release is not stable");
        String version=normalizeVersion(stringValue(root,"tag_name"));if(version==null)throw new IllegalArgumentException("release tag is not semantic");
        URI page=validatedPage(stringValue(root,"html_url"));String expectedName="Skyveil-"+version+".jar";Asset selected=null;
        if(root.has("assets")&&root.get("assets").isJsonArray())for(var element:root.getAsJsonArray("assets")){
            if(!element.isJsonObject())continue;JsonObject asset=element.getAsJsonObject();if(!expectedName.equals(stringValue(asset,"name")))continue;
            long size=longValue(asset,"size");String digest=stringValue(asset,"digest");URI download=validatedDownload(stringValue(asset,"browser_download_url"));
            if(size<=0||size>MAX_JAR_BYTES)throw new IllegalArgumentException("release JAR size is invalid");
            if(digest==null||!digest.matches("(?i)^sha256:[0-9a-f]{64}$"))throw new IllegalArgumentException("release JAR has no SHA-256 digest");
            selected=new Asset(expectedName,download,size,digest.substring(7).toLowerCase(Locale.ROOT));break;
        }
        if(selected==null)throw new IllegalArgumentException("release has no "+expectedName+" asset");
        return new ReleaseInfo(version,page,extractNotes(stringValue(root,"body")),selected);
    }

    static int compareVersions(String left,String right){
        int[] a=versionParts(left),b=versionParts(right);for(int index=0;index<3;index++){int compared=Integer.compare(a[index],b[index]);if(compared!=0)return compared;}return 0;
    }

    static List<String> extractNotes(String markdown){
        if(markdown==null||markdown.isBlank())return List.of();ArrayList<String> notes=new ArrayList<>();
        for(String raw:markdown.lines().toList()){
            String line=raw.trim();if(line.matches("(?i)^#{1,6}\\s+requirements.*"))break;
            if(line.startsWith("- ")&&line.length()>2){notes.add(line.substring(2).replace("`","").trim());if(notes.size()==6)break;}
        }
        return List.copyOf(notes);
    }

    private static void checkAsync(boolean reportResult){
        if(!CHECKING.compareAndSet(false,true)){if(reportResult)chatLater("An update check is already running…",ChatFormatting.YELLOW);return;}
        Thread.startVirtualThread(()->{
            try{
                HttpRequest request=HttpRequest.newBuilder(LATEST_RELEASE).timeout(Duration.ofSeconds(20)).header("Accept","application/vnd.github+json").header("User-Agent","Skyveil/"+CURRENT_VERSION+" updater").GET().build();
                HttpResponse<String> response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));if(response.statusCode()!=200)throw new IllegalStateException("GitHub returned HTTP "+response.statusCode());
                ReleaseInfo release=parseRelease(JsonParser.parseString(response.body()).getAsJsonObject());latest=release;
                boolean available=compareVersions(release.version(),CURRENT_VERSION)>0;LOGGER.info("Latest stable Skyveil release is {} (installed {}, update available: {})",release.version(),CURRENT_VERSION,available);if(available)announcePending=true;
                if(reportResult)Minecraft.getInstance().execute(()->{if(available){announcePending=false;showAvailable(Minecraft.getInstance(),release);}else chat(Minecraft.getInstance(),"Skyveil "+CURRENT_VERSION+" is already current.",ChatFormatting.GREEN);});
            }catch(Exception exception){LOGGER.warn("Could not check GitHub for Skyveil updates",exception);if(reportResult)chatLater("Could not check for updates. Try again later.",ChatFormatting.RED);}
            finally{CHECKING.set(false);}
        });
    }

    private static void downloadAsync(Minecraft client,ReleaseInfo release){
        if(!DOWNLOADING.compareAndSet(false,true))return;chat(client,"Downloading Skyveil "+release.version()+"…",ChatFormatting.YELLOW);
        Thread.startVirtualThread(()->{
            try{
                Path temporary=download(release);verifyJar(temporary,release);install(temporary,release.asset().name());installedForRestart=true;
                Minecraft.getInstance().execute(()->chat(Minecraft.getInstance(),"Skyveil "+release.version()+" installed. Restart Minecraft to finish the update.",ChatFormatting.GREEN));
            }catch(Exception exception){LOGGER.error("Could not install Skyveil {}",release.version(),exception);chatLater("Update failed validation or installation; the current JAR was kept.",ChatFormatting.RED);}
            finally{DOWNLOADING.set(false);}
        });
    }

    private static Path download(ReleaseInfo release)throws Exception{
        Path directory=FabricLoader.getInstance().getConfigDir().resolve("skyveil").resolve("updates");Files.createDirectories(directory);
        Path temporary=directory.resolve(release.asset().name()+".download");Files.deleteIfExists(temporary);
        HttpRequest request=HttpRequest.newBuilder(release.asset().download()).timeout(Duration.ofMinutes(2)).header("Accept","application/octet-stream").header("User-Agent","Skyveil/"+CURRENT_VERSION+" updater").GET().build();
        HttpResponse<Path> response=HTTP.send(request,HttpResponse.BodyHandlers.ofFile(temporary));if(response.statusCode()!=200)throw new IllegalStateException("download returned HTTP "+response.statusCode());
        if(Files.size(temporary)!=release.asset().size())throw new IllegalStateException("download size does not match GitHub metadata");return temporary;
    }

    private static void verifyJar(Path path,ReleaseInfo release)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=Files.newInputStream(path);DigestInputStream checked=new DigestInputStream(input,digest)){byte[] buffer=new byte[16_384];while(checked.read(buffer)>=0){/* digest while streaming */}}
        String actual=HexFormat.of().formatHex(digest.digest());if(!actual.equalsIgnoreCase(release.asset().sha256()))throw new IllegalStateException("download SHA-256 does not match GitHub metadata");
        try(JarFile jar=new JarFile(path.toFile())){
            var entry=jar.getJarEntry("fabric.mod.json");if(entry==null)throw new IllegalStateException("download has no Fabric metadata");
            try(Reader reader=new java.io.InputStreamReader(jar.getInputStream(entry),StandardCharsets.UTF_8)){
                JsonObject metadata=JsonParser.parseReader(reader).getAsJsonObject();
                if(!"skyveil".equals(stringValue(metadata,"id")))throw new IllegalStateException("download is not Skyveil");
                if(!release.version().equals(stringValue(metadata,"version")))throw new IllegalStateException("download version does not match release tag");
            }
            if(jar.getJarEntry("name/skyveil/client/SkyveilClientEntrypoint.class")==null)throw new IllegalStateException("download is missing the Skyveil client entrypoint");
        }
    }

    private static void install(Path downloaded,String assetName)throws Exception{
        Path current=currentJar().orElseThrow(()->new IllegalStateException("the installed Skyveil JAR could not be located"));Path directory=current.getParent();
        if(directory==null||!Files.isDirectory(directory))throw new IllegalStateException("the installed mod directory is unavailable");
        Path target=directory.resolve(assetName),disabled=directory.resolve(current.getFileName()+".replaced-"+System.currentTimeMillis());
        if(!target.equals(current)&&Files.exists(target))throw new IllegalStateException("another "+assetName+" already exists in the mods folder");
        move(current,disabled);boolean committed=false;
        try{move(downloaded,target);committed=true;}finally{if(!committed)move(disabled,current);}
        try{Files.deleteIfExists(disabled);}catch(Exception locked){disabled.toFile().deleteOnExit();LOGGER.info("Old Skyveil JAR will be removed when Java exits: {}",disabled);}
    }

    private static Optional<Path> currentJar(){
        return FabricLoader.getInstance().getModContainer("skyveil").stream().flatMap(container->container.getOrigin().getPaths().stream())
            .map(Path::toAbsolutePath).map(Path::normalize).filter(Files::isRegularFile).filter(path->path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).findFirst();
    }
    private static void move(Path source,Path target)throws Exception{try{Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException unsupported){Files.move(source,target);}}

    private static void showAvailable(Minecraft client,ReleaseInfo release){
        if(client.player==null||release==null)return;
        client.player.sendSystemMessage(Component.literal("[Skyveil] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal("Update "+release.version()+" is available").withStyle(ChatFormatting.GOLD)));
        for(String note:release.notes())client.player.sendSystemMessage(Component.literal(" • "+note).withStyle(ChatFormatting.WHITE));
        client.player.sendSystemMessage(Component.literal("[Download and install]").withStyle(style->style.withColor(ChatFormatting.GREEN).withUnderlined(true)
            .withClickEvent(new ClickEvent.RunCommand("/sv update")).withHoverEvent(new HoverEvent.ShowText(Component.literal("Download, verify, and install Skyveil "+release.version()))))
            .append(Component.literal("  ")).append(Component.literal("[View release]").withStyle(style->style.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(release.page())))));
    }
    private static void chatLater(String message,ChatFormatting color){Minecraft.getInstance().execute(()->chat(Minecraft.getInstance(),message,color));}
    private static void chat(Minecraft client,String message,ChatFormatting color){if(client.player!=null)client.player.sendSystemMessage(Component.literal("[Skyveil] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(message).withStyle(color)));}
    private static URI validatedPage(String value){URI uri=URI.create(value);if(!"https".equalsIgnoreCase(uri.getScheme())||!"github.com".equalsIgnoreCase(uri.getHost())||!uri.getPath().startsWith("/arkutaken/Skyveil/releases/"))throw new IllegalArgumentException("release page URL is outside the Skyveil repository");return uri;}
    private static URI validatedDownload(String value){URI uri=URI.create(value);if(!"https".equalsIgnoreCase(uri.getScheme())||!"github.com".equalsIgnoreCase(uri.getHost())||!uri.getPath().startsWith(RELEASE_PATH_PREFIX))throw new IllegalArgumentException("asset URL is outside the Skyveil repository");return uri;}
    private static String normalizeVersion(String value){if(value==null)return null;String normalized=value.startsWith("v")?value.substring(1):value;return normalized.matches("\\d+\\.\\d+\\.\\d+")?normalized:null;}
    private static int[] versionParts(String value){String normalized=normalizeVersion(value);if(normalized==null)throw new IllegalArgumentException("invalid semantic version: "+value);String[] pieces=normalized.split("\\.");return new int[]{Integer.parseInt(pieces[0]),Integer.parseInt(pieces[1]),Integer.parseInt(pieces[2])};}
    private static String stringValue(JsonObject object,String key){try{return object.get(key).getAsString();}catch(Exception ignored){return null;}}
    private static boolean booleanValue(JsonObject object,String key){try{return object.get(key).getAsBoolean();}catch(Exception ignored){return false;}}
    private static long longValue(JsonObject object,String key){try{return object.get(key).getAsLong();}catch(Exception ignored){return -1;}}

    record ReleaseInfo(String version,URI page,List<String> notes,Asset asset){}
    record Asset(String name,URI download,long size,String sha256){}
}
