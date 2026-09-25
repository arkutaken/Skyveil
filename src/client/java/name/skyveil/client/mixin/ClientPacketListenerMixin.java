package name.skyveil.client.mixin;

import name.skyveil.client.SkyblockSession;
import name.skyveil.client.bestiary.BestiaryChatFilter;
import name.skyveil.client.pet.AutoPetRuleMessageFilter;
import name.skyveil.client.pet.PetTracker;
import name.skyveil.client.hunting.AttributeMenuPanel;
import name.skyveil.client.hunting.AttributeSyphonChatTracker;
import name.skyveil.client.hunting.HuntingBoxValuePanel;
import name.skyveil.client.storage.StoragePreviewManager;
import name.skyveil.client.equipment.EquipmentShortcutRow;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes authoritative server updates only after vanilla has applied them to client state. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // Consume only pongs owned by the performance probe; other responses remain vanilla's.
    @Inject(method="handlePongResponse",at=@At("HEAD"),cancellable=true)
    private void skyveil$performancePong(net.minecraft.network.protocol.ping.ClientboundPongResponsePacket packet,CallbackInfo ci){
        if(name.skyveil.client.performance.PerformanceHud.onPong(packet.time()))ci.cancel();
    }
    @Inject(method="handleSetTime",at=@At("TAIL"))
    private void skyveil$performanceTime(net.minecraft.network.protocol.game.ClientboundSetTimePacket packet,CallbackInfo ci){
        name.skyveil.client.performance.PerformanceHud.onTimeUpdate(packet.gameTime());
    }

    @Inject(method="handleRespawn",at=@At("TAIL"))
    private void skyveil$performanceRespawn(net.minecraft.network.protocol.game.ClientboundRespawnPacket packet,CallbackInfo ci){
        name.skyveil.client.performance.PerformanceHud.reset();
    }
    @Inject(
        method="handleSystemChat",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift=At.Shift.AFTER
        ),
        cancellable=true
    )
    private void skyveil$filterSystemMessages(ClientboundSystemChatPacket packet,CallbackInfo ci){
        if(!SkyblockSession.isActive())return;
        // Tracking runs before either filter, so hiding the Autopet line cannot delay pet state.
        PetTracker.onSystemMessage(packet.content(),packet.overlay());
        AttributeSyphonChatTracker.onSystemMessage(packet.content(),packet.overlay());
        if(AutoPetRuleMessageFilter.shouldSuppress(packet.content(),packet.overlay())
            ||BestiaryChatFilter.shouldSuppress(packet.content(),packet.overlay()))ci.cancel();
    }

    @Inject(method="handlePlayerInfoUpdate",at=@At("TAIL"))
    private void skyveil$playerInfoUpdated(ClientboundPlayerInfoUpdatePacket packet,CallbackInfo ci){if(SkyblockSession.isActive())PetTracker.onTabListPacket("PLAYER_INFO_UPDATE");}

    @Inject(method="handlePlayerInfoRemove",at=@At("TAIL"))
    private void skyveil$playerInfoRemoved(ClientboundPlayerInfoRemovePacket packet,CallbackInfo ci){if(SkyblockSession.isActive())PetTracker.onTabListPacket("PLAYER_INFO_REMOVE");}

    @Inject(method="handleSetPlayerTeamPacket",at=@At("TAIL"))
    private void skyveil$playerTeamUpdated(ClientboundSetPlayerTeamPacket packet,CallbackInfo ci){if(SkyblockSession.isActive())PetTracker.onTabListPacket("PLAYER_TEAM");}

    @Inject(method="handleTabListCustomisation",at=@At("TAIL"))
    private void skyveil$tabListUpdated(ClientboundTabListPacket packet,CallbackInfo ci){if(SkyblockSession.isActive())PetTracker.onTabListPacket("TAB_HEADER_FOOTER");}

    // Invalidate observations after vanilla applies slot contents. Managers defer
    // parsing until the menu settles instead of reading half-delivered pages.
    @Inject(method="handleContainerSetSlot",at=@At("TAIL"))
    private void skyveil$containerSlotUpdate(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){if(!SkyblockSession.isActive())return;PetTracker.onContainerPacket(packet.getContainerId(),"SET_SLOT");AttributeMenuPanel.onContainerUpdate(packet.getContainerId());HuntingBoxValuePanel.onContainerUpdate(packet.getContainerId());StoragePreviewManager.onContainerUpdate(packet.getContainerId());EquipmentShortcutRow.onContainerUpdate(packet.getContainerId());}

    @Inject(method="handleContainerContent",at=@At("TAIL"))
    private void skyveil$containerContentUpdate(ClientboundContainerSetContentPacket packet,CallbackInfo ci){if(!SkyblockSession.isActive())return;PetTracker.onContainerPacket(packet.containerId(),"SET_CONTENT");AttributeMenuPanel.onContainerUpdate(packet.containerId());HuntingBoxValuePanel.onContainerUpdate(packet.containerId());StoragePreviewManager.onContainerUpdate(packet.containerId());EquipmentShortcutRow.onContainerUpdate(packet.containerId());}
}
