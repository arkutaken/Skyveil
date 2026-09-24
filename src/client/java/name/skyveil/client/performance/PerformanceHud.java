package name.skyveil.client.performance;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Screenshot-style FPS, latency, and estimated server tick rate. */
public final class PerformanceHud {
    private static final TpsEstimator TPS=new TpsEstimator();
    private static final PingMeasurement PING=new PingMeasurement();
    private static final int GRAY=0xAAAAAA;
    private PerformanceHud(){}

    public static void register(){
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("performance"),(graphics,delta)->{
            var client=Minecraft.getInstance();
            if(ConfigManager.get().performance.enabled&&HudVisibility.shouldRender(client))HudVisibility.render(graphics,()->render(graphics,client,false));
        });
    }

    public static void reset(){TPS.reset();PING.reset();}
    public static void tick(Minecraft client){
        if(!ConfigManager.get().performance.enabled||client.player==null||client.getConnection()==null)return;
        long token=PING.request(System.nanoTime());
        if(token!=0)client.getConnection().send(new net.minecraft.network.protocol.ping.ServerboundPingRequestPacket(token));
    }
    public static boolean onPong(long token){return PING.receive(token,System.nanoTime());}
    public static void onTimeUpdate(long gameTime){TPS.update(gameTime,System.nanoTime());}
    public static int contentWidth(Minecraft client){return client.font.width("Ping: 9999 ms")+10;}
    public static int contentHeight(){return 39;}
    public static void renderPreview(GuiGraphicsExtractor graphics,Minecraft client){render(graphics,client,true);}

    private static void render(GuiGraphicsExtractor graphics,Minecraft client,boolean preview){
        var c=ConfigManager.get().performance;
        int fps=preview?607:client.getFps();
        int ping=preview?132:PING.value(System.nanoTime());
        double tps=preview?20:TPS.value(System.nanoTime());

        float scale=(float)c.scale;
        int x=Math.round(Math.min(c.hudX,Math.max(0,graphics.guiWidth()-contentWidth(client)*scale)));
        int y=Math.round(Math.min(c.hudY,Math.max(0,graphics.guiHeight()-contentHeight()*scale)));
        graphics.pose().pushMatrix();
        try{
            graphics.pose().translate(x,y);
            graphics.pose().scale(scale,scale);
            graphics.fill(0,0,contentWidth(client),contentHeight(),HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND));
            row(graphics,client,"FPS: ",Integer.toString(fps),5,0xFFFFFFFF);
            row(graphics,client,"Ping: ",ping<0?"--":ping+" ms",16,ping>=120?0xFFFF5555:0xFFFFFFFF);
            row(graphics,client,"TPS: ",formatTps(tps),27,0xFFFFFFFF);
        }finally{graphics.pose().popMatrix();}
    }

    private static void row(GuiGraphicsExtractor graphics,Minecraft client,String label,String value,int y,int color){
        graphics.text(client.font,label,5,y,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.ACCENT),true);
        graphics.text(client.font,value,5+client.font.width(label),y,HudVisibility.color(color),true);
    }
    static String formatTps(double tps){
        if(!Double.isFinite(tps))return "--";
        return tps>=19.95?"20":String.format(Locale.ROOT,"%.1f",tps);
    }
}