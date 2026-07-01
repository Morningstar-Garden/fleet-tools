package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Suppresses the "X left the game" broadcast for vanished players, so they disconnect
// silently. Vanish state persists across the disconnect, so the player is still marked
// vanished at this point.
@Mixin(ServerGamePacketListenerImpl.class)
public class VanishQuitMessageMixin {

    @Shadow
    public ServerPlayer player;

    @Redirect(
            method = "removePlayerFromWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void fleettools_silentQuit(PlayerList instance, Component message, boolean overlay) {
        if (!VanishManager.isVanished(this.player.getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }
}
