package name.skyveil.client.mining;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.DisplaySlot;

public final class CrystalHollowsMapHud {
    public static final int WIDTH=148, HEIGHT=180;
    private static final int LEFT=5, TOP=17, SIZE=138;
    private static Object world;
    private static boolean inHollows;
    private static int npcTicks;
    private static final CrystalHollowsDiscoveries discoveries=new CrystalHollowsDiscoveries();
    private CrystalHollowsMapHud(){}
    public static void reset(){world=null;inHollows=false;npcTicks=0;discoveries.clear();}
    public static void tick(Minecraft client){
        if(!SkyblockSession.isActive()||client.level==null||!ConfigManager.get().crystalHollowsMap.enabled){
            reset();return;
        }
        if(world!=client.level){reset();world=client.level;}
        if(client.player==null)return;
        String location=null;
        inHollows=false;
        var board=client.level.getScoreboard();
        var objective=board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if(objective!=null)for(var entry:board.listPlayerScores(objective)){
            var team=board.getPlayersTeam(entry.owner());
            String line=team==null?entry.ownerName().getString():team.getFormattedName(entry.ownerName()).getString();
            String found=CrystalHollowsMap.location(line);
            if(found==null&&entry.display()!=null)found=CrystalHollowsMap.location(entry.display().getString());
            if(found!=null){inHollows=true;location=found;break;}
        }
        if(!inHollows&&client.getConnection()!=null)
            inHollows=CommissionsHud.widgetLines(client).stream().anyMatch(CrystalHollowsMap::isLocation);
        if(inHollows){
            discoveries.observe(location,client.player.getX(),client.player.getY(),client.player.getZ());
            if(npcTicks++%10==0)for(var entity:client.level.entitiesForRendering()){
                if(entity.isRemoved()||!entity.hasCustomName()
                    ||!(entity instanceof net.minecraft.world.entity.decoration.ArmorStand
                        ||entity instanceof net.minecraft.world.entity.Mob))continue;
                discoveries.observeKing(entity.getCustomName().getString(),entity.getX(),entity.getY(),entity.getZ());
            }
        }
    }
    public static void register(){
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.BEFORE_GIZMOS.register(CrystalHollowsMapHud::renderWaypoints);
        HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("crystal_hollows_map"),(g,delta)->{
            var client=Minecraft.getInstance();
            if(ConfigManager.get().crystalHollowsMap.enabled&&SkyblockSession.isActive()&&inHollows
                &&world==client.level&&HudVisibility.shouldRenderOutsidePlayerList(client))
                HudVisibility.render(g,()->render(g,false));
        });
    }
    public static int x(int screenWidth){
        var c=ConfigManager.get().crystalHollowsMap;
        return Math.max(0,Math.min(c.hudX,(int)(screenWidth-WIDTH*c.scale)));
    }
    public static int y(int screenHeight){
        var c=ConfigManager.get().crystalHollowsMap;
        return Math.max(0,Math.min(c.hudY,(int)(screenHeight-HEIGHT*c.scale)));
    }
    private static int point(double coordinate){return (int)Math.round(CrystalHollowsMap.fraction(coordinate)*(SIZE-1));}
    public static void render(GuiGraphicsExtractor g,boolean preview){
        var client=Minecraft.getInstance();
        if(!preview&&client.player==null)return;
        boolean live=client.player!=null&&inHollows&&world==client.level;
        double px=live?client.player.getX():380,py=live?client.player.getY():110,pz=live?client.player.getZ():350;
        double yaw=Math.toRadians(live?client.player.getYRot():-45);
        var c=ConfigManager.get().crystalHollowsMap;
        g.pose().pushMatrix();
        try{
            g.pose().translate(x(g.guiWidth()),y(g.guiHeight()));
            g.pose().scale((float)c.scale,(float)c.scale);
            fill(g,0,0,WIDTH,HEIGHT,name.skyveil.client.gui.SkyveilTheme.HUD_BACKGROUND);
            text(g,"Crystal Hollows",5,4,0xFFFFAA00);
            text(g,"N",WIDTH-12,4,0xFFFFFFFF);
            int middle=point(512);
            fill(g,LEFT,TOP,LEFT+middle,TOP+middle,0xFF315D43);
            fill(g,LEFT+middle,TOP,LEFT+SIZE,TOP+middle,0xFF326D70);
            fill(g,LEFT,TOP+middle,LEFT+middle,TOP+SIZE,0xFF826132);
            fill(g,LEFT+middle,TOP+middle,LEFT+SIZE,TOP+SIZE,0xFF485A7D);
            text(g,"Jungle",LEFT+5,TOP+9,0xFFFFFFFF);
            text(g,"Mithril",LEFT+middle+5,TOP+9,0xFFFFFFFF);
            text(g,"Deposits",LEFT+middle+5,TOP+19,0xFFFFFFFF);
            text(g,"Goblin",LEFT+5,TOP+SIZE-25,0xFFFFFFFF);
            text(g,"Holdout",LEFT+5,TOP+SIZE-15,0xFFFFFFFF);
            text(g,"Precursor",LEFT+middle+5,TOP+SIZE-25,0xFFFFFFFF);
            text(g,"Remnants",LEFT+middle+5,TOP+SIZE-15,0xFFFFFFFF);
            int n=point(450),end=point(560);
            fill(g,LEFT+n,TOP+n,LEFT+end,TOP+end,0xFF885E94);
            text(g,"N",LEFT+point(505)-2,TOP+point(505)-4,0xFFFFDDFF);
            for(var entry:discoveries.entries()){
                int sx=LEFT+point(entry.x()),sz=TOP+point(entry.z());
                g.outline(sx-3,sz-3,7,7,HudVisibility.color(0xFF101010));
                fill(g,sx-2,sz-2,sx+3,sz+3,entry.site().color);
                text(g,entry.site().letter,Math.min(sx+5,LEFT+SIZE-7),Math.max(TOP,sz-4),entry.site().color);
            }
            // A single arrowhead rotates around the player's projected position.
            int mx=LEFT+point(px),mz=TOP+point(pz);
            g.pose().pushMatrix();
            try{
                g.pose().translate(mx,mz);
                g.pose().rotate((float)yaw);
                // Local +Y is south, matching Minecraft yaw zero.
                for(int row=-3;row<=5;row++){
                    int half=(5-row)/2;
                    fill(g,-half-1,row-1,half+2,row+2,0xFF101010);
                }
                for(int row=-3;row<=5;row++){
                    int half=(5-row)/2;
                    fill(g,-half,row,half+1,row+1,0xFFFFFFFF);
                }
            }finally{g.pose().popMatrix();}
            text(g,CrystalHollowsMap.region(px,py,pz),5,158,0xFFFFFFFF);
            text(g,"X "+(int)Math.floor(px)+"  Y "+(int)Math.floor(py)+"  Z "+(int)Math.floor(pz),5,169,0xFFBBBBCC);
        }finally{g.pose().popMatrix();}
    }
    private static void renderWaypoints(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context){
        var client=Minecraft.getInstance();
        if(!ConfigManager.get().crystalHollowsMap.enabled||!SkyblockSession.isActive()||!inHollows
            ||world!=client.level||client.player==null||!HudVisibility.shouldRenderOutsidePlayerList(client))return;
        var collection=context.levelRenderer().collectPerFrameGizmos();
        try{
            for(var entry:discoveries.entries()){
                var pos=new net.minecraft.world.phys.Vec3(entry.x(),entry.y(),entry.z());
                int color=entry.site().color;
                net.minecraft.gizmos.Gizmos.cuboid(new net.minecraft.world.phys.AABB(
                    entry.x()-.35,entry.y(),entry.z()-.35,entry.x()+.35,entry.y()+.7,entry.z()+.35),
                    net.minecraft.gizmos.GizmoStyle.strokeAndFill(color,2f,(color&0xFFFFFF)|0x30000000)).setAlwaysOnTop();
                String label=entry.site().label+(entry.site()==CrystalHollowsDiscoveries.Site.KING?" [":" entry [")+Math.round(client.player.position().distanceTo(pos))+"m]";
                net.minecraft.gizmos.Gizmos.billboardText(label,pos.add(0,1.2,0),
                    net.minecraft.gizmos.TextGizmo.Style.forColorAndCentered(color).withScale(1f)).setAlwaysOnTop();
            }
        }finally{collection.close();}
    }
    private static void fill(GuiGraphicsExtractor g,int x,int y,int right,int bottom,int color){
        g.fill(x,y,right,bottom,HudVisibility.color(color));
    }
    private static void text(GuiGraphicsExtractor g,String text,int x,int y,int color){
        g.text(Minecraft.getInstance().font,text,x,y,HudVisibility.color(color),true);
    }
}
