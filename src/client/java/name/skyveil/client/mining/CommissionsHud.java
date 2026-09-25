package name.skyveil.client.mining;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import java.util.*;

/**
 * Polls commission widget rows on the client thread and draws the latest parse.
 * Data remains world/connection-scoped. Hiding for TAB affects rendering only,
 * so tracking continues while the player reads the expanded list.
 */
public final class CommissionsHud {
    public static final int WIDTH=220,HEIGHT=116;
    private static final Comparator<PlayerInfo> TAB_ORDER=Comparator
        .comparingInt((PlayerInfo info)->-info.getTabListOrder())
        .thenComparingInt(info->info.getGameMode()==GameType.SPECTATOR?1:0)
        .thenComparing(info->info.getTeam()==null?"":info.getTeam().getName())
        .thenComparing(info->info.getProfile().name(),String.CASE_INSENSITIVE_ORDER);
    private static final List<CommissionParser.Commission> PREVIEW=List.of(
        new CommissionParser.Commission("Mithril Miner","42.5%",.425),
        new CommissionParser.Commission("Titanium Miner","75%",.75),
        new CommissionParser.Commission("Goblin Slayer","Done",1),
        new CommissionParser.Commission("Royal Mines Mithril","18%",.18));
    private static List<CommissionParser.Commission> commissions=List.of();
    private static Object world,connection;
    private static int ticks;
    private CommissionsHud(){}
    public static void reset(){commissions=List.of();world=null;connection=null;ticks=0;}
    public static void tick(Minecraft client){
        if(!SkyblockSession.isActive()||!ConfigManager.get().commissions.enabled||client.level==null||client.getConnection()==null){reset();return;}
        if(world!=client.level||connection!=client.getConnection()){
            reset();world=client.level;connection=client.getConnection();
        }
        // Widget text need not be reparsed every rendered frame; five client ticks
        // keep updates responsive while bounding sorting/parsing work.
        if(ticks++%5!=0)return;
        commissions=CommissionParser.parse(widgetLines(client));
    }
    // Preserve the displayed TAB order: section parsers rely on neighboring rows.
    static List<String> widgetLines(Minecraft client){
        var overlay=client.gui.getTabList();
        return client.getConnection().getListedOnlinePlayers().stream()
            .sorted(TAB_ORDER).limit(80).map(info->overlay.getNameForDisplay(info).getString()).toList();
    }
    public static void register(){
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("commissions"),(graphics,delta)->{
            var client=Minecraft.getInstance();
            if(ConfigManager.get().commissions.enabled&&SkyblockSession.isActive()&&world==client.level
                &&!commissions.isEmpty()&&HudVisibility.shouldRenderOutsidePlayerList(client))
                HudVisibility.render(graphics,()->render(graphics,false));
        });
    }
    public static int x(int width){
        var c=ConfigManager.get().commissions;
        return Math.max(0,Math.min(c.hudX,(int)(width-WIDTH*c.scale)));
    }
    public static int y(int height){
        var c=ConfigManager.get().commissions;
        return Math.max(0,Math.min(c.hudY,(int)(height-HEIGHT*c.scale)));
    }
    public static void render(GuiGraphicsExtractor graphics,boolean preview){
        var client=Minecraft.getInstance();var c=ConfigManager.get().commissions;
        var rows=preview?PREVIEW:commissions;if(rows.isEmpty())return;
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x(graphics.guiWidth()),y(graphics.guiHeight()));
            graphics.pose().scale((float)c.scale,(float)c.scale);
            graphics.fill(0,0,WIDTH,20+rows.size()*16,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND));
            graphics.text(client.font,"Commissions",5,5,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.ACCENT),true);
            int y=19;
            for(var row:rows){
                String text=row.name()+": "+row.progress();
                float fit=Math.min(1,(WIDTH-10)/(float)Math.max(1,client.font.width(text)));
                int color=HudVisibility.color(row.fraction()>=1?name.skyveil.client.gui.SkyveilTheme.SUCCESS:0xFFFFFFFF);
                graphics.pose().pushMatrix();
                try{graphics.pose().translate(5,y);graphics.pose().scale(fit,fit);graphics.text(client.font,text,0,0,color,true);}
                finally{graphics.pose().popMatrix();}
                graphics.fill(5,y+10,WIDTH-5,y+12,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.BAR_TRACK));
                graphics.fill(5,y+10,5+(int)((WIDTH-10)*row.fraction()),y+12,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.SUCCESS));
                y+=16;
            }
        }finally{graphics.pose().popMatrix();}
    }
}
