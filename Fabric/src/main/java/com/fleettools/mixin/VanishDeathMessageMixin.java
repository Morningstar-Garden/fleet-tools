package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Suppresses death-message broadcasts for vanished players (both the normal and the
// team-scoped paths). The player still gets their own death screen; only the public
// announcement is withheld.
@Mixin(ServerPlayer.class)
public class VanishDeathMessageMixin {

    @Redirect(
            method = "die",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void fleettools_silentDeath(PlayerList instance, Component message, boolean overlay) {
        if (!VanishManager.isVanished(((ServerPlayer) (Object) this).getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }

    @Redirect(
            method = "die",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void fleettools_silentDeathTeam(PlayerList instance, Player teamPlayer, Component message) {
        if (!VanishManager.isVanished(((ServerPlayer) (Object) this).getUUID())) {
            instance.broadcastSystemToTeam(teamPlayer, message);
        }
    }
}
