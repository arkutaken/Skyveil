package name.skyveil.client.map;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reuses skins Minecraft already loaded for nearby player-backed Hypixel NPC entities. */
public final class NpcSkinCache {
    private static final Map<String,Identifier> SKINS=new ConcurrentHashMap<>();
    private static ClientLevel trackedLevel;
    private static int cooldown;
    private NpcSkinCache() {}
    public static Identifier skin(String name){return SKINS.get(normalize(name));}
    public static void tick(Minecraft client){
        if(client.level!=trackedLevel){trackedLevel=client.level;SKINS.clear();cooldown=0;}
        var config=ConfigManager.get().map;
        if(!config.enabled||!config.npcsEnabled||!config.minimapNpcs&&!config.largeNpcs){if(!SKINS.isEmpty())SKINS.clear();cooldown=0;return;}
        if(cooldown-->0)return;cooldown=20;if(client.level==null)return;
        SkyblockMapDefinition map=MapManager.current();if(map==null)return;
        for(AbstractClientPlayer player:client.level.players()){
            String display=player.getDisplayName().getString();String normalized=normalize(display);
            for(var npc:map.npcs){
                String npcName=normalize(npc.name);double dx=player.getX()-npc.x,dy=player.getY()-npc.y,dz=player.getZ()-npc.z;
                boolean named=normalized.equals(npcName)||normalized.endsWith(" "+npcName);
                boolean exactlyAtNpc=player!=client.player&&dx*dx+dy*dy+dz*dz<=2.25;
                if(named||exactlyAtNpc)SKINS.put(npcName,player.getSkin().body().texturePath());
            }
        }
    }
    private static String normalize(String value){return value.replaceAll("\\[[^]]+]","").replaceAll("[^A-Za-z0-9 ]","").replaceAll("\\s+"," ").trim().toLowerCase(Locale.ROOT);}
}
