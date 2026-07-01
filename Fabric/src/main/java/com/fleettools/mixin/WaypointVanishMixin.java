package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// Hides vanished players from the locator bar (the experience-bar compass shown with
// multiple players online). A waypoint connection is only formed when the transmitter
// returns a connection for a given receiver, so a vanished player returns none to any
// receiver who isn't allowed to see them. Staff with fleettools.vanish.see still get it.
@Mixin(LivingEntity.class)
public class WaypointVanishMixin {

    @Inject(method = "makeWaypointConnectionWith", at = @At("HEAD"), cancellable = true)
    private void fleettools_hideVanishedWaypoint(ServerPlayer receiver,
                                                 CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        if ((Object) this instanceof ServerPlayer transmitter
                && VanishManager.isVanished(transmitter.getUUID())
                && !VanishManager.canSee(receiver)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
