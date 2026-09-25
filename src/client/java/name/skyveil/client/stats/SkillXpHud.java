package name.skyveil.client.stats;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SkillXpHud {
    public static final int WIDTH=210,HEIGHT=28;
    private static SkillXpParser.Reading latest;
    private static long received;
    private static Object world;
    private SkillXpHud(){}
    public static void reset(){latest=null;world=null;}
    public static void register(){
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("skill_xp"),(graphics,delta)->{
            var client=Minecraft.getInstance();
            if(ConfigManager.get().skillXp.enabled&&SkyblockSession.isActive()&&HudVisibility.shouldRender(client)
                &&client.level==world&&latest!=null&&System.nanoTime()-received<3_000_000_000L)HudVisibility.render(graphics,()->render(graphics,false));
        });
    }
    public static Component onActionBar(Component text){
        if(!ConfigManager.get().skillXp.enabled||!SkyblockSession.isActive())return text;
        var reading=SkillXpParser.parse(text.getString());if(reading==null)return text;
        latest=reading;received=System.nanoTime();world=Minecraft.getInstance().level;
        var remaining=Component.empty();int offset=0;
        for(var part:text.toFlatList()){
            String content=part.getString();StringBuilder kept=new StringBuilder();
            for(int i=0;i<content.length();i++)if(offset+i<reading.start()||offset+i>=reading.end())kept.append(content.charAt(i));
            if(!kept.isEmpty())remaining.append(Component.literal(kept.toString()).setStyle(part.getStyle()));
            offset+=content.length();
        }
        return remaining;
    }
    public static int x(int width){
        var c=ConfigManager.get().skillXp;
        return Math.max(0,Math.min(c.hudX<0?width/2-(int)(WIDTH*c.scale/2):c.hudX,(int)(width-WIDTH*c.scale)));
    }
    public static int y(int height){
        var c=ConfigManager.get().skillXp;
        return Math.max(0,Math.min(c.hudY<0?height-155:c.hudY,(int)(height-HEIGHT*c.scale)));
    }
    public static void render(GuiGraphicsExtractor graphics,boolean preview){
        var client=Minecraft.getInstance();var c=ConfigManager.get().skillXp;
        var value=preview?new SkillXpParser.Reading("Farming","+24","158,094/217,000",158094d/217000,0,0):latest;
        if(value==null)return;
        // Keep the notification visible for three seconds, fading during the last
        // second. Layout previews use full opacity and do not depend on recent XP.
        int alpha=preview?255:(int)(255*Math.min(1,(3_000_000_000L-(System.nanoTime()-received))/1_000_000_000d));
        if(alpha<=0)return;
        int color=(alpha<<24)|(name.skyveil.client.gui.SkyveilTheme.ACCENT&0xFFFFFF);
        color=HudVisibility.color(color);
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x(graphics.guiWidth()),y(graphics.guiHeight()));
            graphics.pose().scale((float)c.scale,(float)c.scale);
            graphics.fill(0,0,WIDTH,HEIGHT,HudVisibility.color(((alpha*153/255)<<24)|(name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND&0xFFFFFF)));
            line(graphics,client,value.skill()+" "+value.gain()+" XP",0,color);
            line(graphics,client,value.progress().isEmpty()?"":value.progress(),11,HudVisibility.color((alpha<<24)|0xFFFFFF));
            graphics.fill(3,24,WIDTH-3,26,HudVisibility.color((alpha<<24)|(name.skyveil.client.gui.SkyveilTheme.BAR_TRACK&0xFFFFFF)));
            if(value.fraction()>=0)graphics.fill(3,24,3+(int)((WIDTH-6)*value.fraction()),26,HudVisibility.color((alpha<<24)|(name.skyveil.client.gui.SkyveilTheme.SUCCESS&0xFFFFFF)));
        }finally{graphics.pose().popMatrix();}
    }
    private static void line(GuiGraphicsExtractor graphics,Minecraft client,String text,int y,int color){
        float fit=Math.min(1,(WIDTH-2)/(float)Math.max(1,client.font.width(text)+1));
        graphics.pose().pushMatrix();
        try{graphics.pose().translate(1,y);graphics.pose().scale(fit,fit);graphics.text(client.font,text,0,0,color,true);}
        finally{graphics.pose().popMatrix();}
    }
}