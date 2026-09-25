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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigInteger;

/** Client-only lifecycle observer and label replacement for confidently classified damage splashes. */
public final class CompactDamageManager {
    private static final double PLAYER_RANGE_SQUARED=64.0*64.0;
    private static final double TARGET_RADIUS=2.75;
    private static final double MULTIPART_LINK_RADIUS=3.5;
    private static final int MAX_MULTIPART_PARTS=64;
    private static final int MAX_SEEN_IDS=512;
    private static final long MELEE_RESPONSE_TICKS=12;
    private static final DamageBatcher BATCHES=new DamageBatcher();
    private static final Set<Integer> SEEN=new LinkedHashSet<>();
    private static final Set<Integer> SUPPRESSED=new HashSet<>();
    private static final Map<Integer,MeleeAttack> MELEE_ATTACKS=new HashMap<>();
    private static ClientLevel trackedLevel;
    private static Object trackedConnection;
    private CompactDamageManager(){}

    public static void onMeleeAttack(Entity entity){
        if(!(entity instanceof LivingEntity living)||living instanceof Player||living instanceof ArmorStand||!enabled())return;
        Minecraft client=Minecraft.getInstance();if(client.level==null||living.level()!=client.level)return;
        LivingEntity target=canonicalTarget(living);MELEE_ATTACKS.put(target.getId(),new MeleeAttack(client.level.getGameTime(),wearingCrimson(client),null,false));
    }

    // Entity IDs can be reported repeatedly as metadata changes. Count a recognized
    // splash once, and associate it with a nearby living target before aggregating.
    public static void onEntityAddedOrUpdated(Entity entity){
        if(!(entity instanceof ArmorStand stand)||!enabled()||SEEN.contains(entity.getId()))return;
        Minecraft client=Minecraft.getInstance();if(client==null||client.level==null||client.player==null||stand.level()!=client.level)return;
        if(!stand.isInvisible()||!stand.hasCustomName()||!stand.isCustomNameVisible()||stand.distanceToSqr(client.player)>PLAYER_RANGE_SQUARED)return;
        Component name=stand.getCustomName();if(name==null)return;var parsed=DamageTextParser.parse(name.getString());if(parsed.isEmpty())return;
        LivingEntity target=nearestTarget(stand);long now=client.level.getGameTime();if(target==null)return;target=canonicalTarget(target);
        SEEN.add(stand.getId());while(SEEN.size()>MAX_SEEN_IDS){Integer oldest=SEEN.iterator().next();SEEN.remove(oldest);SUPPRESSED.remove(oldest);}
        DamageSource source=resolveSource(name,target.getId(),parsed.get().value(),now);
        // Compact Damage owns every recognized damage splash. Disabled sources are
        // hidden but ignored; enabled secondary sources are merged into a melee sample.
        SUPPRESSED.add(stand.getId());
        if(!included(source))return;
        if(source==DamageSource.MELEE)BATCHES.accept(target.getId(),parsed.get().value(),now);else BATCHES.addSecondary(target.getId(),parsed.get().value(),now);
    }

    public static void onEntityRemoved(int entityId){SEEN.remove(entityId);SUPPRESSED.remove(entityId);MELEE_ATTACKS.remove(entityId);BATCHES.removeTarget(entityId);}

    public static void tick(Minecraft client){
        if(client.level==null||client.player==null){clear();return;}
        if(client.level!=trackedLevel||client.getConnection()!=trackedConnection){clear();trackedLevel=client.level;trackedConnection=client.getConnection();}
        if(!enabled()){if(!SEEN.isEmpty()||BATCHES.batchCount()>0)clearWorldData();return;}
        long now=client.level.getGameTime();BATCHES.tick(now);MELEE_ATTACKS.values().removeIf(attack->now-attack.tick>MELEE_RESPONSE_TICKS);
    }

    /** Every recognized source label is hidden; the target-attached renderer publishes the sole replacement. */
    public static boolean shouldSuppress(ArmorStand stand){return enabled()&&SUPPRESSED.contains(stand.getId());}
    static java.util.List<DamageBatcher.Display> displays(){return enabled()?BATCHES.displays():java.util.List.of();}

    public static void clear(){clearWorldData();trackedLevel=null;trackedConnection=null;}
    private static void clearWorldData(){SEEN.clear();SUPPRESSED.clear();MELEE_ATTACKS.clear();BATCHES.clear();}
    private static boolean enabled(){return ConfigManager.get().compactDamage;}
    private static DamageSource resolveSource(Component name,int targetId,BigInteger damage,long tick){
        DamageSource styled=DamageSourceClassifier.styledSource(name);if(styled!=DamageSource.MELEE)return styled;
        MeleeAttack attack=MELEE_ATTACKS.get(targetId);if(attack==null||tick-attack.tick<0||tick-attack.tick>MELEE_RESPONSE_TICKS)return DamageSource.OTHER;
        if(!attack.primaryAccepted){attack.primaryAccepted=true;attack.primaryDamage=damage;return DamageSource.MELEE;}
        return DamageSourceClassifier.physicalFollowUp(attack.primaryDamage,damage,attack.crimsonEquipped);
    }
    private static boolean included(DamageSource source){var config=ConfigManager.get();return switch(source){
        case MELEE->true;case CRIMSON_SWIPE->config.compactDamageCrimsonSwipe;case FEROCITY->config.compactDamageFerocity;
        case VENOMOUS->config.compactDamageVenomous;case FIRE->config.compactDamageFire;case THUNDERLORD->config.compactDamageThunderlord;
        case PET->config.compactDamagePet;case OTHER->config.compactDamageOther;};
    }
    private static boolean wearingCrimson(Minecraft client){int pieces=0;for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET})if(client.player.getItemBySlot(slot).getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("crimson"))pieces++;return pieces>=2;}
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
    private static final class MeleeAttack {final long tick;final boolean crimsonEquipped;BigInteger primaryDamage;boolean primaryAccepted;MeleeAttack(long tick,boolean crimsonEquipped,BigInteger primaryDamage,boolean primaryAccepted){this.tick=tick;this.crimsonEquipped=crimsonEquipped;this.primaryDamage=primaryDamage;this.primaryAccepted=primaryAccepted;}}
}
