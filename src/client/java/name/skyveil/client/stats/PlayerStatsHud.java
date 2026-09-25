package name.skyveil.client.stats;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.config.SkyveilConfig;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.EnumMap;
import java.util.Locale;

/**
 * Retains recognized action-bar readings and renders independently positioned
 * stats. A temporary skill/ability message is not evidence that a stat vanished;
 * world/session changes, rather than unrelated messages, clear these readings.
 */
public final class PlayerStatsHud {
    public static final int WIDTH=116,HEIGHT=17;
    private static final int ICON_SPACE=15,BAR_TOP=12,BAR_HEIGHT=2;
    private static final EnumMap<PlayerStat,StatParser.Reading> VALUES=new EnumMap<>(PlayerStat.class);
    private static Object world;
    private PlayerStatsHud(){}
    public static boolean active(){return ConfigManager.get().playerStats.enabled&&SkyblockSession.isActive();}
    public static SkyveilConfig.StatPosition position(PlayerStat stat){return ConfigManager.get().playerStats.positions.get(stat);}
    public static void register(){
        // Observe the original overlay before MODIFY_GAME handlers can strip stats.
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.ALLOW_GAME.register((message,overlay)->{
            if(overlay)observe(message);
            return true;
        });
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("player_stats"),(graphics,delta)->{
            var client=Minecraft.getInstance();
            if(!active()||!HudVisibility.shouldRender(client))return;
            HudVisibility.render(graphics,()->{for(var stat:PlayerStat.values())render(graphics,stat,false);});
        });
    }
    public static void reset(){VALUES.clear();world=null;}
    public static void tick(Minecraft client){
        if(!SkyblockSession.isActive()||client.level!=world){reset();world=client.level;}
        if(!active()||client.player==null)return;
        // The client movement speed includes sprinting; remove its vanilla multiplier for the stat.
        double speed=client.player.getSpeed()*1000/(client.player.isSprinting()?1.3:1);
        VALUES.put(PlayerStat.SPEED,new StatParser.Reading(PlayerStat.SPEED,speed,0,0,0));
    }
    private static void observe(Component message){
        if(!active())return;
        var client=Minecraft.getInstance();
        if(client.level!=world){reset();world=client.level;}
        for(var reading:StatParser.parse(message.getString()))VALUES.put(reading.stat(),reading);
    }
    public static Component onActionBar(Component message){
        if(!active())return message;
        var client=Minecraft.getInstance();
        if(client.level!=world){reset();world=client.level;}
        String plain=message.getString();
        var readings=StatParser.parse(plain);
        if(readings.isEmpty())return message;
        boolean[] removed=new boolean[plain.length()];
        for(var reading:readings){
            VALUES.put(reading.stat(),reading);
            for(int i=reading.start();i<reading.end();i++)removed[i]=true;
        }
        // Preserve styles and all text outside recognized stats, including ability and skill messages.
        var remaining=Component.empty();
        int offset=0;
        for(var part:message.toFlatList()){
            String text=part.getString();
            StringBuilder kept=new StringBuilder();
            for(int i=0;i<text.length();i++)if(offset+i<removed.length&&!removed[offset+i])kept.append(text.charAt(i));
            if(!kept.isEmpty())remaining.append(Component.literal(kept.toString()).setStyle(part.getStyle()));
            offset+=text.length();
        }
        return remaining;
    }
    // Editor sizing uses real readings when available, with preview values only
    // for missing data. Normal gameplay rendering still hides unknown stats.
    private static StatParser.Reading displayValue(PlayerStat stat){
        var value=VALUES.get(stat);return value==null?preview(stat):value;
    }
    private static String number(PlayerStat stat,StatParser.Reading value){
        return format(value.value())+(stat.resource?"/"+format(value.maximum()):"")
            +(value.overflow()>0?" +"+format(value.overflow()):"");
    }
    /**
     * Unscaled width shared by drawing, clamping, and the layout editor.
     * Resource bars stay fixed; numeric-only stats fit the current formatted value.
     */
    public static int contentWidth(PlayerStat stat){
        if(stat.resource)return WIDTH;
        return ICON_SPACE+Minecraft.getInstance().font.width(number(stat,displayValue(stat)))+4;
    }
    public static int x(PlayerStat stat,int screenWidth){
        var p=position(stat);
        int width=contentWidth(stat);
        int base=switch(stat){case HEALTH,DEFENSE->screenWidth/2-width-4;case MANA,SPEED->screenWidth/2+4;case VITALITY->screenWidth/2-width/2;};
        return Math.max(0,Math.min(p.hudX<0?base:p.hudX,(int)(screenWidth-width*p.scale)));
    }
    public static int contentHeight(PlayerStat stat){return stat.resource?HEIGHT:12;}
    public static int y(PlayerStat stat,int screenHeight){
        var p=position(stat);
        int base=screenHeight-switch(stat){case HEALTH,MANA->65;case DEFENSE,SPEED->93;case VITALITY->121;};
        return Math.max(0,Math.min(p.hudY<0?base:p.hudY,(int)(screenHeight-contentHeight(stat)*p.scale)));
    }
    public static void render(GuiGraphicsExtractor graphics,PlayerStat stat,boolean preview){
        var client=Minecraft.getInstance();var p=position(stat);
        StatParser.Reading value=preview?displayValue(stat):VALUES.get(stat);
        // Temporary ability/skill messages do not invalidate the last server reading.
        // Never show an empty resource bar for a stat the server has not reported.
        if(!preview&&value==null)return;
        String number=number(stat,value);
        int width=contentWidth(stat);
        int color=stat==PlayerStat.HEALTH&&value!=null&&value.value()>value.maximum()?0xFFFFAA00:stat.color;
        color=HudVisibility.color(color);
        float scale=(float)p.scale;
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x(stat,graphics.guiWidth()),y(stat,graphics.guiHeight()));
            graphics.pose().scale(scale,scale);
            if(!stat.resource)graphics.fill(0,0,width,contentHeight(stat),HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND));
            fittedText(graphics,client,stat.icon,1,1,11,10,color);
            fittedText(graphics,client,number,ICON_SPACE,1,width-ICON_SPACE-1,10,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.TEXT));
            if(stat.resource){
                graphics.fill(3,BAR_TOP,WIDTH-3,BAR_TOP+BAR_HEIGHT,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.BAR_TRACK));
                if(value!=null){
                    int fill=(int)Math.round((WIDTH-6)*value.fraction());
                    if(fill>0){
                        graphics.fill(3,BAR_TOP,3+fill,BAR_TOP+BAR_HEIGHT,color);
                    }
                }
            }

        }finally{graphics.pose().popMatrix();}
    }
    /** Fit visible glyph pixels (including bearings and shadow), not just their advance width. */
    private static void fittedText(GuiGraphicsExtractor graphics,Minecraft client,String text,
                                   int x,int y,int width,int height,int color){
        var bounds=client.font.prepareText(text,0,0,color,true,0).bounds();
        if(bounds==null||bounds.width()<=0||bounds.height()<=0)return;
        float fit=Math.min(1,Math.min(width/(float)bounds.width(),height/(float)bounds.height()));
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x-bounds.left()*fit,y+(height-bounds.height()*fit)/2-bounds.top()*fit);
            graphics.pose().scale(fit,fit);
            graphics.text(client.font,text,0,0,color,true);
        }finally{graphics.pose().popMatrix();}
    }
    private static String format(double value){return String.format(Locale.ROOT,"%,.0f",value);}
    private static StatParser.Reading preview(PlayerStat stat){
        return switch(stat){
            case HEALTH->new StatParser.Reading(stat,4703,4328,0,0);
            case MANA->new StatParser.Reading(stat,599,599,0,0);
            case VITALITY->new StatParser.Reading(stat,123,123,0,0);
            case DEFENSE->new StatParser.Reading(stat,387,0,0,0);
            case SPEED->new StatParser.Reading(stat,400,0,0,0);
        };
    }
}