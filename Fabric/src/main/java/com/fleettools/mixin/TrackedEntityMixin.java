package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

// Hides vanished players' entities from players who aren't allowed to see them. When
// the tracker updates a viewer, a vanished-and-not-visible entity is un-paired (sending
// a remove packet if needed) and the normal add is skipped.
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class TrackedEntityMixin {
    @Shadow @Final private ServerEntity serverEntity;
    @Shadow @Final private Entity entity;
    @Shadow @Final private Set<ServerPlayerConnection> seenBy;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void fleettools_hideVanished(ServerPlayer player, CallbackInfo ci) {
        if (entity instanceof ServerPlayer tracked
                && VanishManager.isVanished(tracked.getUUID())
                && !VanishManager.canSee(player)) {
            if (seenBy.remove(player.connection)) {
                serverEntity.removePairing(player);
            }
            ci.cancel();
        }
    }
}
