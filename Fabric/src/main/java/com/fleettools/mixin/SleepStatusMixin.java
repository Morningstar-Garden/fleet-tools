package com.fleettools.mixin;

import com.fleettools.events.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

// Excludes AFK players from the sleep-percentage calculation, so AFK players don't
// block the night from being skipped (and weather from clearing).
@Mixin(SleepStatus.class)
public class SleepStatusMixin {

    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true)
    private List<ServerPlayer> fleettools_excludeAfk(List<ServerPlayer> players) {
        return players.stream().filter(player -> !AfkManager.isAfk(player.getUUID())).toList();
    }
}
