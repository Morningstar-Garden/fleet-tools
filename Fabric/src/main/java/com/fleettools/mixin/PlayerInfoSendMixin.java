package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Robust tab-list vanish: filters vanished players out of every outgoing player-info
// packet destined for a viewer who isn't allowed to see them. This runs on the actual
// send path, so it survives relogs, gamemode/latency broadcasts, and third-party tab
// managers (StyledPlayerList) that would otherwise re-add the vanished entry.
@Mixin(net.minecraft.server.network.ServerCommonPacketListenerImpl.class)
public class PlayerInfoSendMixin {

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1)
    private Packet<?> fleettools_filterVanished(Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket update)) {
            return packet;
        }
        // Only ServerGamePacketListenerImpl has an associated player (the viewer).
        if (!((Object) this instanceof ServerGamePacketListenerImpl listener)) {
            return packet;
        }
        ServerPlayer viewer = listener.player;
        if (viewer == null || VanishManager.canSee(viewer)) {
            return packet;
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> entries = update.entries();
        UUID viewerId = viewer.getUUID();
        boolean needsFilter = false;
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : entries) {
            if (!entry.profileId().equals(viewerId) && VanishManager.isVanished(entry.profileId())) {
                needsFilter = true;
                break;
            }
        }
        if (!needsFilter) {
            return packet;
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> filtered = new ArrayList<>(entries.size());
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : entries) {
            if (entry.profileId().equals(viewerId) || !VanishManager.isVanished(entry.profileId())) {
                filtered.add(entry);
            }
        }

        // Build a fresh packet (never mutate the shared instance sent to other viewers):
        // the empty-collection constructor keeps the original actions, then we swap in
        // the filtered entry list via the accessor.
        ClientboundPlayerInfoUpdatePacket copy =
                new ClientboundPlayerInfoUpdatePacket(update.actions(), List.<ServerPlayer>of());
        ((PlayerInfoUpdatePacketAccessor) (Object) copy).fleettools$setEntries(List.copyOf(filtered));
        return copy;
    }
}
