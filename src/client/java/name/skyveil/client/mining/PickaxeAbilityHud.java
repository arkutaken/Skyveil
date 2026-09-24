package name.skyveil.client.mining;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PickaxeAbilityHud {
    private static final int PADDING=3;
    private static PickaxeAbilityParser.Reading reading;
    private static Object world,connection;
    private static int ticks;
    private PickaxeAbilityHud(){}
    public static void reset(){reading=null;world=null;connection=null;ticks=0;}
    public static void tick(Minecraft client){
        if(!SkyblockSession.isActive()||!ConfigManager.get().pickaxeAbility.enabled||client.level==null||client.getConnection()==null){reset();return;}
        if(world!=client.level||connection!=client.getConnection()){
            reset();world=client.level;connection=client.getConnection();
        }
        if(ticks++%5==0)reading=PickaxeAbilityParser.parse(CommissionsHud.widgetLines(client));
    }
    public static void register(){
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("pickaxe_ability"),(graphics,delta)->{
            var client=Minecraft.getInstance();
            if(ConfigManager.get().pickaxeAbility.enabled&&SkyblockSession.isActive()&&world==client.level
                &&reading!=null&&HudVisibility.shouldRenderOutsidePlayerList(client))
                HudVisibility.render(graphics,()->render(graphics,false));
        });
    }
    private static PickaxeAbilityParser.Reading value(boolean preview){
        return reading!=null?reading:preview?new PickaxeAbilityParser.Reading("Mining Speed Boost","42s",false):null;
    }
    private static String status(PickaxeAbilityParser.Reading value){
        return value.ready()?"Ready":value.status();
    }
    public static int width(boolean preview){
        var value=value(preview);
        if(value==null)return 0;
        return Minecraft.getInstance().font.width(value.ability()+": "+status(value))+PADDING*2;
    }
    public static int height(){return Minecraft.getInstance().font.lineHeight+PADDING*2;}
    public static int x(int screenWidth,boolean preview){
        var c=ConfigManager.get().pickaxeAbility;
        return Math.max(0,Math.min(c.hudX,(int)(screenWidth-width(preview)*c.scale)));
    }
    public static int y(int screenHeight){
        var c=ConfigManager.get().pickaxeAbility;
        return Math.max(0,Math.min(c.hudY,(int)(screenHeight-height()*c.scale)));
    }
    public static void render(GuiGraphicsExtractor graphics,boolean preview){
        var value=value(preview);
        if(value==null)return;
        var client=Minecraft.getInstance();var c=ConfigManager.get().pickaxeAbility;
        String title=value.ability()+": ";
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x(graphics.guiWidth(),preview),y(graphics.guiHeight()));
            graphics.pose().scale((float)c.scale,(float)c.scale);
            graphics.fill(0,0,width(preview),height(),HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND));
            graphics.text(client.font,title,PADDING,PADDING,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.ACCENT),true);
            graphics.text(client.font,status(value),PADDING+client.font.width(title),PADDING,
                HudVisibility.color(value.ready()?name.skyveil.client.gui.SkyveilTheme.SUCCESS:0xFFFFFFFF),true);
        }finally{graphics.pose().popMatrix();}
    }
}
