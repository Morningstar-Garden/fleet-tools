package com.fleettools.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.data.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerJoinHandler {

    // Last-known flight state, used to keep a flying player airborne across gamemode changes.
    private static final Map<UUID, Boolean> WAS_FLYING = new HashMap<>();

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

        // Catch-all reconciliation: anything that resets a player's abilities (gamemode
        // changes, respawns, other mods) is corrected on the next tick, so stored fly /
        // god-mode never silently break.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                reconcileAbilities(player);
            }
        });

        // Save player's location when they disconnect.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerDataManager.setLastLocation(handler.getPlayer());
            WAS_FLYING.remove(handler.getPlayer().getUUID());
        });
    }

    public static void restoreAbilities(ServerPlayer player) {
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

    // Re-applies stored fly/god only when the ability has been cleared, so it costs a
    // packet only on the tick after something reset it (not every tick). If the player
    // was actively flying, flight is kept across the change instead of just the ability.
    private static void reconcileAbilities(ServerPlayer player) {
        var abilities = player.getAbilities();
        UUID id = player.getUUID();
        boolean wasFlying = WAS_FLYING.getOrDefault(id, false);
        boolean changed = false;

        if (PlayerDataManager.getFlyEnabled(player) && !abilities.mayfly) {
            abilities.mayfly = true;
            if (wasFlying && !abilities.flying) {
                abilities.flying = true; // keep them airborne through the gamemode change
            }
            changed = true;
        }

        if (PlayerDataManager.getGodMode(player) && !abilities.invulnerable) {
            abilities.invulnerable = true;
            changed = true;
        }

        if (changed) {
            player.onUpdateAbilities();
        }

        // Remember the current flight state (after any restoration) for next tick.
        WAS_FLYING.put(id, abilities.flying);
    }
}
