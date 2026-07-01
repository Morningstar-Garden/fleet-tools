package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Suppresses the "X joined the game" broadcast for players who are (still) vanished when
// they connect, so a vanished staff member rejoins silently.
@Mixin(PlayerList.class)
public class VanishJoinMessageMixin {

    @Redirect(
            method = "placeNewPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void fleettools_silentJoin(PlayerList instance, Component message, boolean overlay,
                                       Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        if (!VanishManager.isVanished(player.getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }
}
