package name.skyveil.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;

import java.util.Locale;

/** Central authority for deciding whether Skyveil gameplay features may run. */
public final class SkyblockSession {
    private static Object connection;
    private static boolean hypixel;
    private static boolean active;

    private SkyblockSession() {}

    public static void tick(Minecraft client) {
        Object liveConnection=client.getConnection();
        if(liveConnection!=connection) {
            connection=liveConnection;
            active=false;
            hypixel=liveConnection!=null&&matchesHypixelAddress(currentServerAddress(client));
        } else if(liveConnection!=null&&!hypixel) {
            // ServerData may become available one tick after a connection replacement.
            hypixel=matchesHypixelAddress(currentServerAddress(client));
        }
        if(liveConnection==null||client.player==null||client.level==null||!hypixel) {
            active=false;
            return;
        }
        var sidebar=client.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        active=sidebar!=null&&matchesSkyblockTitle(sidebar.getDisplayName().getString());
    }

    public static boolean isActive(){return active;}

    public static void disconnect(){connection=null;hypixel=false;active=false;}

    static boolean matchesSkyblockTitle(String title) {
        return title!=null&&title.toUpperCase(Locale.ROOT).contains("SKYBLOCK");
    }

    static boolean matchesHypixelAddress(String address) {
        if(address==null||address.isBlank())return false;
        String host=address.trim().toLowerCase(Locale.ROOT);
        if(host.startsWith("[")) {
            int end=host.indexOf(']');
            host=end<0?host:host.substring(1,end);
        } else {
            int port=host.lastIndexOf(':');
            if(port>=0)host=host.substring(0,port);
        }
        return host.equals("hypixel.net")||host.endsWith(".hypixel.net");
    }

    private static String currentServerAddress(Minecraft client) {
        var server=client.getCurrentServer();
        return server==null?"":server.ip;
    }
}
