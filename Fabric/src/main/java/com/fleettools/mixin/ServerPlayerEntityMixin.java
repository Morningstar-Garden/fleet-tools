package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerEntity.class, priority = 500)
public class ServerPlayerEntityMixin {

    // NOTE: The teleport method signature changed in 1.21.11 and is no longer easily hookable
    // The /back command will still work - last locations are saved by other commands like /home, /spawn, /warp
    // If needed, can hook into individual teleport commands instead
    
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void fleettools_onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        
        // Check if player has god mode enabled
        if (PlayerDataManager.getGodMode(player)) {
            // Cancel damage if player is in god mode
            cir.setReturnValue(false);
        }
    }
}
