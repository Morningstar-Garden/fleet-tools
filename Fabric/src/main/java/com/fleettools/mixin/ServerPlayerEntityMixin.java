package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, priority = 500)
public class ServerPlayerEntityMixin {

    // Capture vanilla /tp and coordinate teleports for /back (mojmap teleportTo signature)
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"))
    private void fleettools_onTeleport(net.minecraft.server.level.ServerLevel destination, double x, double y, double z, java.util.Set<?> movementFlags, float yaw, float pitch, boolean setCamera, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerDataManager.setLastLocation(player);
    }

    // Capture world change teleports (e.g., /tp <player> <dim>) via the dimension TeleportTransition
    @Inject(method = "teleport", at = @At("HEAD"))
    private void fleettools_onMoveToWorld(net.minecraft.world.level.portal.TeleportTransition transition, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerDataManager.setLastLocation(player);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void fleettools_onDamage(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        
        // Check if player has god mode enabled
        if (PlayerDataManager.getGodMode(player)) {
            // Cancel damage if player is in god mode
            cir.setReturnValue(false);
        }
    }

    // Render the player-list name with the LuckPerms prefix, grayed with an [AFK] tag
    // while AFK. Skipped when StyledPlayerList is present (it owns the list).
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void fleettools_tabName(CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        net.minecraft.network.chat.Component base = cir.getReturnValue() != null ? cir.getReturnValue() : player.getName();
        net.minecraft.network.chat.Component custom = com.fleettools.events.AfkManager.tabDisplayName(player, base);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
