package name.skyveil.client.hunting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact recognition of Hypixel's paged Hunting Box container. */
public final class HuntingBoxMenuDetector {
    private static final Pattern TITLE=Pattern.compile("^(?:\\(\\s*(\\d+)\\s*/\\s*(\\d+)\\s*\\)\\s*)?hunting box$",Pattern.CASE_INSENSITIVE);
    private HuntingBoxMenuDetector(){}
    public static boolean matches(AbstractContainerScreen<?> screen){
        if(screen==null||!TITLE.matcher(normalize(screen.getTitle().getString())).matches())return false;
        Minecraft client=Minecraft.getInstance();int serverSlots=0;
        for(var slot:screen.getMenu().slots)if(client.player==null||slot.container!=client.player.getInventory())serverSlots++;
        return serverSlots>=54;
    }
    public static Page page(AbstractContainerScreen<?> screen){return parsePage(screen==null?"":screen.getTitle().getString());}
    static Page parsePage(String title){
        Matcher matcher=TITLE.matcher(normalize(title));if(!matcher.matches()||matcher.group(1)==null)return new Page(1,1);
        try{return new Page(Math.max(1,Integer.parseInt(matcher.group(1))),Math.max(1,Integer.parseInt(matcher.group(2))));}
        catch(NumberFormatException ignored){return new Page(1,1);}
    }
    private static String normalize(String value){return value==null?"":value.replaceAll("(?:\\u00c2)?\\u00a7.","").replaceAll("\\s+"," ").trim();}
    public record Page(int current,int total){}
}
