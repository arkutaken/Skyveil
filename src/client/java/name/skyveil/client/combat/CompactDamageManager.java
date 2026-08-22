package name.skyveil.client.combat;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client-only lifecycle observer and label replacement for confidently classified damage splashes. */
public final class CompactDamageManager {
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-compact-damage");
    private static final double PLAYER_RANGE_SQUARED=64.0*64.0;
    private static final double TARGET_RADIUS=2.75;
    private static final double MULTIPART_LINK_RADIUS=3.5;
    private static final int MAX_MULTIPART_PARTS=64;
    private static final int MAX_SEEN_IDS=512;
    private static final DamageBatcher BATCHES=new DamageBatcher();
    private static final Set<Integer> SEEN=new LinkedHashSet<>();
    private static ClientLevel trackedLevel;
    private static Object trackedConnection;
    private CompactDamageManager(){}

    public static void onEntityAddedOrUpdated(Entity entity){
        if(!(entity instanceof ArmorStand stand)||!enabled()||SEEN.contains(entity.getId()))return;
        Minecraft client=Minecraft.getInstance();if(client==null||client.level==null||client.player==null||stand.level()!=client.level)return;
        if(!stand.isInvisible()||!stand.hasCustomName()||!stand.isCustomNameVisible()||stand.distanceToSqr(client.player)>PLAYER_RANGE_SQUARED)return;
        Component name=stand.getCustomName();if(name==null)return;var parsed=DamageTextParser.parse(name.getString());if(parsed.isEmpty())return;
        LivingEntity target=nearestTarget(stand);long now=client.level.getGameTime();if(target==null)return;target=canonicalTarget(target);
        SEEN.add(stand.getId());while(SEEN.size()>MAX_SEEN_IDS)SEEN.remove(SEEN.iterator().next());
        DamageBatcher.Batch batch=BATCHES.accept(target.getId(),parsed.get().value(),now);
        if(ConfigManager.get().map.debug)LOGGER.info("Accepted damage indicator entity={} target={} value={} window={} average={}",stand.getId(),target.getId(),parsed.get().value(),batch.hits.size(),batch.average());
    }

    public static void onEntityRemoved(int entityId){SEEN.remove(entityId);BATCHES.removeTarget(entityId);}

    public static void tick(Minecraft client){
        if(client.level==null||client.player==null){clear();return;}
        if(client.level!=trackedLevel||client.getConnection()!=trackedConnection){clear();trackedLevel=client.level;trackedConnection=client.getConnection();}
        if(!enabled()){if(!SEEN.isEmpty()||BATCHES.batchCount()>0)clearWorldData();return;}
        BATCHES.tick(client.level.getGameTime());
    }

    /** Every accepted source label is hidden; the target-attached renderer publishes the sole replacement. */
    public static boolean shouldSuppress(ArmorStand stand){return enabled()&&SEEN.contains(stand.getId());}
    static java.util.List<DamageBatcher.Display> displays(){return enabled()?BATCHES.displays():java.util.List.of();}

    public static void clear(){clearWorldData();trackedLevel=null;trackedConnection=null;}
    private static void clearWorldData(){SEEN.clear();BATCHES.clear();}
    private static boolean enabled(){return ConfigManager.get().compactDamage;}
    private static LivingEntity nearestTarget(ArmorStand stand){
        AABB area=stand.getBoundingBox().inflate(TARGET_RADIUS);Minecraft client=Minecraft.getInstance();List<LivingEntity> nearby=stand.level().getEntitiesOfClass(LivingEntity.class,area,entity->entity.isAlive()&&!(entity instanceof ArmorStand)&&entity!=client.player);if(nearby.isEmpty())return null;
        LivingEntity nearest=nearby.stream().min(Comparator.comparingDouble(stand::distanceToSqr)).orElse(null);
        // A damage splash closest to another player is ambiguous and must never create
        // a compact label attached to that player or to a mob standing behind them.
        if(nearest instanceof Player)return null;
        return nearest;
    }

    /** Collapses passenger roots and chains of invisible multipart hitboxes to one stable anchor. */
    private static LivingEntity canonicalTarget(LivingEntity target){
        LivingEntity root=livingVehicleRoot(target);if(root!=target)return root;if(!target.isInvisible())return target;
        ArrayList<LivingEntity> component=new ArrayList<>();ArrayDeque<LivingEntity> pending=new ArrayDeque<>();HashSet<Integer> visited=new HashSet<>();pending.add(target);visited.add(target.getId());
        while(!pending.isEmpty()&&component.size()<MAX_MULTIPART_PARTS){LivingEntity part=pending.removeFirst();component.add(part);AABB links=part.getBoundingBox().inflate(MULTIPART_LINK_RADIUS);for(LivingEntity candidate:part.level().getEntitiesOfClass(LivingEntity.class,links,entity->entity.isAlive()&&entity.isInvisible()&&!(entity instanceof ArmorStand)&&!(entity instanceof Player)&&entity.getType()==target.getType()))if(visited.add(candidate.getId()))pending.addLast(candidate);}
        if(component.size()==1)return target;
        return component.stream().min(Comparator.comparing((LivingEntity entity)->!entity.hasCustomName()).thenComparing((LivingEntity entity)->entity.isInvisible()).thenComparingInt(Entity::getId)).orElse(target);
    }
    private static LivingEntity livingVehicleRoot(LivingEntity target){Entity root=target;while(root.getVehicle()!=null)root=root.getVehicle();return root instanceof LivingEntity living&&living.isAlive()&&!(living instanceof Player)&&!(living instanceof ArmorStand)?living:target;}
}
