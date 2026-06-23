package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;

public class BackOnDeathHandler {

    // Default-on permission node; deny it for a player/group to disable death-return.
    private static final String PERMISSION = "fleettools.back.ondeath";

    public static void register() {
        // Capture the death location at the moment of death, while the player is
        // still at the spot where they died (including the dimension). Doing this
        // here rather than at respawn avoids reading the already-respawned position.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player && Permissions.check(player, PERMISSION, true)) {
                PlayerDataManager.recordDeathLocation(player);
            }
        });

        // After respawn (an actual death, not a return from the End), promote the
        // recorded death location to the player's /back point. Running here means a
        // respawn teleport cannot overwrite it.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                PlayerDataManager.consumeDeathLocationAsBack(newPlayer);
            }
        });
    }
}
