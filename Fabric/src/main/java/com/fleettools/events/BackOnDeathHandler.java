package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class BackOnDeathHandler {

    // Default-on permission node; deny it for a player/group to disable death-return.
    private static final String PERMISSION = "fleettools.back.ondeath";

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive == true means the player returned from the End; we only want deaths.
            if (alive) {
                return;
            }
            // Default to enabled; an explicit "false" on the node opts a player out.
            if (!Permissions.check(newPlayer, PERMISSION, true)) {
                return;
            }
            // The old (dead) player entity still holds the death position, dimension and
            // facing. Running this in AFTER_RESPAWN (after placement) means the respawn
            // teleport can't overwrite the recorded /back point.
            PlayerDataManager.setLastLocation(
                    newPlayer,
                    oldPlayer.position(),
                    oldPlayer.level(),
                    oldPlayer.getYRot(),
                    oldPlayer.getXRot());
        });
    }
}
