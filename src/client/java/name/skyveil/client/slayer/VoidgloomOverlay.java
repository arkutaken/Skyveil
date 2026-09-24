package name.skyveil.client.slayer;

import com.mojang.authlib.properties.Property;
import name.skyveil.client.SkyblockSession;
import name.skyveil.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tracks and renders the three dangerous world mechanics used by Voidgloom Seraph. */
public final class VoidgloomOverlay {
    private static final String NUKEKUBI_TEXTURE="eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwNzU5NGUyZGYyNzM5MjFhNzdjMTAxZDBiZmRmYTExMTVhYmVkNWI5YjIwMjllYjQ5NmNlYmE5YmRiYjRiMyJ9fX0=";
    private static final long BEACON_FUSE_NANOS=5_000_000_000L;
    private static boolean registered;
    private static int scanDelay;
    private static int ownBossId=-1;
    private static List<Laser> lasers=List.of();
    private static List<Integer> heads=List.of(),flyingBeacons=List.of();
    private static Map<Integer,Vec3> lastFlying=Map.of();
    private static final Map<BlockPos,Long> placedBeacons=new LinkedHashMap<>();

    private VoidgloomOverlay(){}

    public static void register(){if(registered)return;registered=true;LevelRenderEvents.BEFORE_GIZMOS.register(VoidgloomOverlay::render);}

    public static void tick(Minecraft client){
        if(!SkyblockSession.isActive()||client.level==null||client.player==null){clear();return;}
        var config=ConfigManager.get().voidgloom;if(!config.highlightLasers&&!config.highlightBeacon&&!config.highlightHeads){clear();return;}
        if(++scanDelay<2)return;scanDelay=0;
        ClientLevel level=client.level;Vec3 player=client.player.position();ArrayList<Entity> nearby=new ArrayList<>();
        if(!hasOwnVoidgloomQuest(level)){ownBossId=-1;clearMechanics();return;}
        for(Entity entity:level.entitiesForRendering())if(!entity.isRemoved()&&entity.position().distanceToSqr(player)<=96*96)nearby.add(entity);

        ArrayList<EnderMan> bosses=new ArrayList<>();ArrayList<Vec3> nameTags=new ArrayList<>();
        for(Entity entity:nearby)if(entity.getName().getString().toLowerCase(Locale.ROOT).contains("voidgloom seraph"))nameTags.add(entity.position());
        for(Entity entity:nearby)if(entity instanceof EnderMan enderman&&(enderman.isPassenger()||nameTags.stream().anyMatch(tag->tag.distanceToSqr(enderman.position())<=25)))bosses.add(enderman);
        Entity locked=ownBossId<0?null:level.getEntity(ownBossId);EnderMan ownBoss=locked instanceof EnderMan enderman&&!enderman.isRemoved()?enderman:null;
        if(ownBossId>=0&&ownBoss==null){clearMechanics();return;}
        if(ownBoss==null){List<EnderMan> fresh=bosses.stream().filter(boss->boss.tickCount<80).toList();List<EnderMan> choices=fresh.isEmpty()?bosses:fresh;ownBoss=choices.stream().min(java.util.Comparator.comparingDouble(boss->boss.distanceToSqr(client.player))).orElse(null);ownBossId=ownBoss==null?-1:ownBoss.getId();}
        if(ownBoss==null){clearMechanics();return;}
        if(!bosses.contains(ownBoss))bosses.add(ownBoss);

        ArrayList<Laser> nextLasers=new ArrayList<>();ArrayList<Integer> nextHeads=new ArrayList<>(),nextFlying=new ArrayList<>();Map<Integer,Vec3> seenFlying=new HashMap<>();
        for(Entity entity:nearby){
            if(config.highlightLasers&&entity instanceof Guardian guardian&&guardian.isInvisible()&&guardian.hasActiveAttackTarget()){
                Entity target=guardian.getActiveAttackTarget();if(target!=null&&nearBoss(guardian,target,ownBoss))nextLasers.add(new Laser(guardian.getId(),target.getId()));
            }
            if(entity instanceof ArmorStand stand){
                if(config.highlightBeacon&&isBeacon(stand)&&belongsToBoss(stand,ownBoss,bosses)){nextFlying.add(stand.getId());seenFlying.put(stand.getId(),stand.position());findPlacedBeacon(level,stand.position());}
                if(config.highlightHeads&&isNukekubi(stand)&&belongsToBoss(stand,ownBoss,bosses))nextHeads.add(stand.getId());
            }
        }
        if(config.highlightBeacon)for(var entry:lastFlying.entrySet())if(!seenFlying.containsKey(entry.getKey()))findPlacedBeacon(level,entry.getValue());
        lasers=List.copyOf(nextLasers);heads=List.copyOf(nextHeads);flyingBeacons=List.copyOf(nextFlying);lastFlying=Map.copyOf(seenFlying);
        long now=System.nanoTime();placedBeacons.entrySet().removeIf(entry->now-entry.getValue()>7_000_000_000L||!level.getBlockState(entry.getKey()).is(Blocks.BEACON));
    }

    private static boolean nearBoss(Entity source,Entity target,EnderMan boss){return source.distanceToSqr(boss)<=36||target.distanceToSqr(boss)<=36;}
    private static boolean belongsToBoss(Entity mechanic,EnderMan own,List<EnderMan> bosses){double ownDistance=mechanic.distanceToSqr(own);if(ownDistance>900)return false;for(EnderMan boss:bosses)if(boss!=own&&mechanic.distanceToSqr(boss)<ownDistance)return false;return true;}
    private static boolean hasOwnVoidgloomQuest(ClientLevel level){
        var scoreboard=level.getScoreboard();var objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);if(objective==null)return false;ArrayList<String> lines=new ArrayList<>();
        for(var entry:scoreboard.listPlayerScores(objective)){lines.add(entry.owner());lines.add(entry.ownerName().getString());if(entry.display()!=null)lines.add(entry.display().getString());var team=scoreboard.getPlayersTeam(entry.owner());if(team!=null)lines.add(team.getFormattedName(entry.ownerName()).getString());}
        return isOwnVoidgloomQuest(lines);
    }
    static boolean isOwnVoidgloomQuest(List<String> lines){boolean voidgloom=false,slay=false;for(String line:lines){String text=line.toLowerCase(Locale.ROOT);voidgloom|=text.contains("voidgloom seraph");slay|=text.contains("slay the boss");}return voidgloom&&slay;}
    private static boolean isBeacon(ArmorStand stand){
        for(EquipmentSlot slot:EquipmentSlot.values()){ItemStack stack=stand.getItemBySlot(slot);if(!stack.isEmpty()&&(stack.is(Blocks.BEACON.asItem())||"Beacon".equals(stack.getHoverName().getString())))return true;}return false;
    }
    private static boolean isNukekubi(ArmorStand stand){
        ItemStack stack=stand.getItemBySlot(EquipmentSlot.HEAD);var profile=stack.get(DataComponents.PROFILE);if(profile==null)return false;
        for(Property property:profile.partialProfile().properties().get("textures"))if(matchesNukekubiTexture(property.value()))return true;return false;
    }
    static boolean matchesNukekubiTexture(String texture){return NUKEKUBI_TEXTURE.equals(texture);}
    private static void findPlacedBeacon(ClientLevel level,Vec3 center){
        BlockPos origin=BlockPos.containing(center);for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){BlockPos pos=origin.offset(x,y,z);if(level.getBlockState(pos).is(Blocks.BEACON)){placedBeacons.putIfAbsent(pos.immutable(),System.nanoTime());return;}}
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context){
        if(!SkyblockSession.isActive())return;Minecraft client=Minecraft.getInstance();if(client.level==null)return;var config=ConfigManager.get().voidgloom;
        var collection=context.levelRenderer().collectPerFrameGizmos();try{
            if(config.highlightLasers)for(Laser laser:lasers){Entity from=client.level.getEntity(laser.source),to=client.level.getEntity(laser.target);if(from!=null&&to!=null)Gizmos.line(from.getEyePosition(),to.getEyePosition(),config.laserColor,3f);}
            if(config.highlightHeads)for(int id:heads){Entity entity=client.level.getEntity(id);if(entity!=null)highlight(entity.getBoundingBox().inflate(.12),config.headColor);}
            if(config.highlightBeacon){
                for(int id:flyingBeacons){Entity entity=client.level.getEntity(id);if(entity!=null){Vec3 p=entity.position();highlight(new AABB(p.x-.5,p.y,p.z-.5,p.x+.5,p.y+1,p.z+.5),config.beaconColor);}}
                long now=System.nanoTime();for(var entry:placedBeacons.entrySet()){BlockPos pos=entry.getKey();highlight(new AABB(pos),config.beaconColor);double remaining=Math.max(0,(BEACON_FUSE_NANOS-(now-entry.getValue()))/1_000_000_000d);Gizmos.billboardText(String.format(Locale.ROOT,"Beacon %.1fs",remaining),Vec3.atCenterOf(pos).add(0,.9,0),TextGizmo.Style.forColorAndCentered(config.beaconColor).withScale(1.2f)).setAlwaysOnTop();}
            }
        }finally{collection.close();}
    }
    private static void highlight(AABB box,int color){int fill=(color&0xFFFFFF)|0x40000000;Gizmos.cuboid(box,GizmoStyle.strokeAndFill(color,2f,fill)).setAlwaysOnTop();}
    private static void clearMechanics(){lasers=List.of();heads=List.of();flyingBeacons=List.of();lastFlying=Map.of();placedBeacons.clear();}
    public static void clear(){scanDelay=0;ownBossId=-1;clearMechanics();}
    private record Laser(int source,int target){}
}
