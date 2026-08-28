package name.skyveil.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.SkyblockSession;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Renders one camera-facing rolling-average label at each active target's live position. */
public final class CompactDamageRenderer {
    private static boolean registered;
    private CompactDamageRenderer(){}
    public static void register(){if(registered)return;registered=true;LevelRenderEvents.COLLECT_SUBMITS.register(CompactDamageRenderer::render);}
    private static void render(LevelRenderContext context){
        if(!SkyblockSession.isActive())return;var camera=context.levelState().cameraRenderState;if(camera==null||camera.pos==null)return;Minecraft client=Minecraft.getInstance();if(client.level==null)return;
        for(DamageBatcher.Display display:CompactDamageManager.displays()){
            var target=client.level.getEntity(display.targetId());if(target==null||target.isRemoved())continue;Vec3 position=target.position();double distance=client.gameRenderer.getMainCamera().position().distanceToSqr(position);
            PoseStack pose=context.poseStack();pose.pushPose();try{pose.translate(position.x-camera.pos.x,position.y-camera.pos.y,position.z-camera.pos.z);
                Component label=label(display,ConfigManager.get().compactDamageStyle);
                context.submitNodeCollector().submitNameTag(pose,new Vec3(0,target.getBbHeight()+1.15,0),0,label,true,0xF000F0,distance,context.levelState().cameraRenderState);
            }finally{pose.popPose();}
        }
    }
    static Component label(DamageBatcher.Display display,String style){
        String damage=DamageTextParser.format(display.average()),progress=" ["+display.hits()+"/"+DamageBatcher.MAX_HITS+"]";
        return switch(style==null?"MINIMAL":style){
            case "NEON"->Component.literal("\u2726 ").withStyle(ChatFormatting.AQUA,ChatFormatting.BOLD).append(Component.literal(damage).withStyle(ChatFormatting.LIGHT_PURPLE,ChatFormatting.BOLD)).append(Component.literal(" \u2726"+progress).withStyle(ChatFormatting.AQUA));
            case "CRIMSON"->Component.literal("\u2694 ").withStyle(ChatFormatting.DARK_RED,ChatFormatting.BOLD).append(Component.literal(damage).withStyle(ChatFormatting.RED,ChatFormatting.BOLD)).append(Component.literal(progress).withStyle(ChatFormatting.GOLD));
            default->Component.literal(damage).withStyle(ChatFormatting.WHITE);
        };
    }
}
