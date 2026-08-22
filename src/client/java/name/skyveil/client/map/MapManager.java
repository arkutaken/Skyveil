package name.skyveil.client.map;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;

/** Selects a registered map from Hypixel's client-visible tab-list Area line. */
public final class MapManager {
    private static SkyblockMapDefinition current;
    private static int cooldown;
    private MapManager() {}
    public static SkyblockMapDefinition current(){return current;}

    public static void tick(Minecraft client){
        if(!ConfigManager.get().map.enabled){current=null;cooldown=0;return;}
        if(cooldown-->0)return;cooldown=20;current=null;
        if(client.getConnection()==null)return;
        for(var info:client.getConnection().getOnlinePlayers()){
            String line=info.getTabListDisplayName()!=null?info.getTabListDisplayName().getString():info.getProfile().name();
            SkyblockMapDefinition matched=SkyblockMapRegistry.matchLocationText(line);
            if(matched!=null){current=matched;return;}
        }
    }
}
