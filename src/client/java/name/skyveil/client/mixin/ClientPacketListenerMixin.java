package name.skyveil.client.mixin;

import name.skyveil.client.bestiary.BestiaryChatFilter;
import name.skyveil.client.pet.AutoPetRuleMessageFilter;
import name.skyveil.client.pet.PetTracker;
import name.skyveil.client.hunting.AttributeMenuPanel;
import name.skyveil.client.hunting.AttributeSyphonChatTracker;
import name.skyveil.client.hunting.HuntingBoxValuePanel;
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
        // Tracking runs before either filter, so hiding the Autopet line cannot delay pet state.
        PetTracker.onSystemMessage(packet.content(),packet.overlay());
        AttributeSyphonChatTracker.onSystemMessage(packet.content(),packet.overlay());
        if(AutoPetRuleMessageFilter.shouldSuppress(packet.content(),packet.overlay())
            ||BestiaryChatFilter.shouldSuppress(packet.content(),packet.overlay()))ci.cancel();
    }

    @Inject(method="handlePlayerInfoUpdate",at=@At("TAIL"))
    private void skyveil$playerInfoUpdated(ClientboundPlayerInfoUpdatePacket packet,CallbackInfo ci){PetTracker.onTabListPacket("PLAYER_INFO_UPDATE");}

    @Inject(method="handlePlayerInfoRemove",at=@At("TAIL"))
    private void skyveil$playerInfoRemoved(ClientboundPlayerInfoRemovePacket packet,CallbackInfo ci){PetTracker.onTabListPacket("PLAYER_INFO_REMOVE");}

    @Inject(method="handleSetPlayerTeamPacket",at=@At("TAIL"))
    private void skyveil$playerTeamUpdated(ClientboundSetPlayerTeamPacket packet,CallbackInfo ci){PetTracker.onTabListPacket("PLAYER_TEAM");}

    @Inject(method="handleTabListCustomisation",at=@At("TAIL"))
    private void skyveil$tabListUpdated(ClientboundTabListPacket packet,CallbackInfo ci){PetTracker.onTabListPacket("TAB_HEADER_FOOTER");}

    @Inject(method="handleContainerSetSlot",at=@At("TAIL"))
    private void skyveil$containerSlotUpdate(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){PetTracker.onContainerPacket(packet.getContainerId(),"SET_SLOT");AttributeMenuPanel.onContainerUpdate(packet.getContainerId());HuntingBoxValuePanel.onContainerUpdate(packet.getContainerId());}

    @Inject(method="handleContainerContent",at=@At("TAIL"))
    private void skyveil$containerContentUpdate(ClientboundContainerSetContentPacket packet,CallbackInfo ci){PetTracker.onContainerPacket(packet.containerId(),"SET_CONTENT");AttributeMenuPanel.onContainerUpdate(packet.containerId());HuntingBoxValuePanel.onContainerUpdate(packet.containerId());}
}
