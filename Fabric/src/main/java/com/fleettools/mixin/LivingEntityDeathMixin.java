package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class)
public class LivingEntityDeathMixin {
    
    @Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
    private void fleettools_onDropLoot(ServerWorld world, DamageSource damageSource, boolean bl, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only handle players
        if (entity instanceof ServerPlayerEntity player) {
            // Check if player has keep inventory enabled
            if (PlayerDataManager.getKeepInventory(player)) {
                // Cancel loot dropping if player has keep inventory enabled
                // This helps with mod compatibility by preventing any additional item drops
                ci.cancel();
            }
        }
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void fleettools_onDrop(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only handle players
        if (entity instanceof ServerPlayerEntity player) {
            // Check if player has keep inventory enabled
            if (PlayerDataManager.getKeepInventory(player)) {
                // Cancel item dropping if player has keep inventory enabled
                ci.cancel();
            }
        }
    }
}