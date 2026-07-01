package com.fleettools.mixin;

import com.fleettools.events.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Prevent mobs from acquiring an AFK player as their attack target, so hostile
// mobs ignore idle players entirely instead of merely failing to damage them
// (the no-damage half lives in ServerPlayerEntityMixin#hurtServer).
@Mixin(Mob.class)
public class MobTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void fleettools_skipAfkTarget(LivingEntity target, CallbackInfo ci) {
        if (target instanceof ServerPlayer player && AfkManager.isProtected(player.getUUID())) {
            ci.cancel();
        }
    }
}
