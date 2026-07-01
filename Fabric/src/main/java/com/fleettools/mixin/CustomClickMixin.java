package com.fleettools.mixin;

import com.fleettools.commands.TeleportRequestCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Handles the custom chat-click actions fired by the teleport-request [Accept]/[Deny]
// buttons. Using a custom click event (rather than run_command) means the client sends
// the action straight here with no "run this command?" confirmation prompt. Vanilla only
// debug-logs unknown custom actions, so we intercept our namespace and cancel.
@Mixin(net.minecraft.server.network.ServerCommonPacketListenerImpl.class)
public class CustomClickMixin {

    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void fleettools_handleTeleportButtons(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        Identifier id = packet.id();
        if (!id.getNamespace().equals("fleettools")) {
            return; // not ours; let vanilla handle it
        }
        // Only the in-game listener has an associated player.
        if (!((Object) this instanceof ServerGamePacketListenerImpl listener)) {
            return;
        }
        ServerPlayer player = listener.player;
        if (player == null) {
            return;
        }

        String requester = "";
        if (packet.payload().orElse(null) instanceof CompoundTag tag) {
            requester = tag.getStringOr(TeleportRequestCommands.PAYLOAD_REQUESTER, "");
        }
        if (requester.isEmpty()) {
            ci.cancel();
            return;
        }

        // This injection runs on the network thread; hop to the server thread before
        // touching game state.
        final String path = id.getPath();
        final String requesterName = requester;
        MinecraftServer server = player.level().getServer();
        server.execute(() -> {
            if (path.equals(TeleportRequestCommands.ACCEPT_ACTION.getPath())) {
                TeleportRequestCommands.acceptRequest(player, requesterName);
            } else if (path.equals(TeleportRequestCommands.DENY_ACTION.getPath())) {
                TeleportRequestCommands.denyRequest(player, requesterName);
            }
        });
        ci.cancel();
    }
}
