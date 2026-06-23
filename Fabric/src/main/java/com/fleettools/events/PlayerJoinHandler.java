package com.fleettools.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.data.PlayerDataManager;

public class PlayerJoinHandler {

    public static void register() {
        // Restore persistent abilities (fly, god mode) when the player joins.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            restoreAbilities(handler.getPlayer());
        });

        // Respawn creates a fresh player entity with default abilities, so re-apply
        // the stored fly / god-mode state (otherwise the player has to toggle them
        // off and on again after dying).
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            restoreAbilities(newPlayer);
        });

        // Save player's location when they disconnect.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDataManager.setLastLocation(handler.getPlayer());
        });
    }

    private static void restoreAbilities(ServerPlayer player) {
        boolean changed = false;

        if (PlayerDataManager.getFlyEnabled(player)) {
            player.getAbilities().mayfly = true;
            changed = true;
        }

        if (PlayerDataManager.getGodMode(player)) {
            player.getAbilities().invulnerable = true;
            changed = true;
        }

        if (changed) {
            player.onUpdateAbilities();
        }
    }
}
