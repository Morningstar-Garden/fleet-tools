package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class)
public class LivingEntityDeathMixin {
    
    // 1.21.x funnels death drops through dropAllDeathLoot(ServerLevel, DamageSource);
    // cancelling it at HEAD prevents any item drops when keep-inventory is enabled.
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void fleettools_onDropLoot(net.minecraft.server.level.ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only handle players
        if (entity instanceof ServerPlayer player) {
            // Cancel all death-loot dropping if player has keep inventory enabled
            if (PlayerDataManager.getKeepInventory(player)) {
                ci.cancel();
            }
        }
    }
}