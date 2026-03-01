package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(value = ServerPlayerEntity.class, priority = 500)
public class ServerPlayerEntityMixin {
    private Vec3d fleettools_lastPos = null;
    private ServerWorld fleettools_lastWorld = null;

    // Track teleportation by monitoring significant position changes during tick
    @Inject(method = "tick", at = @At("TAIL"))
    private void fleettools_onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        
        try {
            Vec3d currentPos = ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos();
            ServerWorld currentWorld = (ServerWorld) ((com.fleettools.mixin.accessor.EntityAccessor) player).getWorld();
            
            if (currentPos != null && currentWorld != null) {
                if (fleettools_lastPos != null && fleettools_lastWorld != null) {
                    // Check if it's a significant teleport (more than 8 blocks distance or different world)
                    double distance = Math.sqrt(Math.pow(currentPos.x - fleettools_lastPos.x, 2) + 
                                              Math.pow(currentPos.y - fleettools_lastPos.y, 2) + 
                                              Math.pow(currentPos.z - fleettools_lastPos.z, 2));
                    boolean differentWorld = !currentWorld.equals(fleettools_lastWorld);
                    
                    if (distance > 8.0 || differentWorld) {
                        // Significant teleport detected - save previous position for /back
                        PlayerDataManager.setLastLocation(player, fleettools_lastPos, fleettools_lastWorld);
                    }
                }
                
                // Update tracked position every 20 ticks (1 second)
                if (player.age % 20 == 0) {
                    fleettools_lastPos = currentPos;
                    fleettools_lastWorld = currentWorld;
                }
            }
        } catch (Exception e) {
            // Silently handle any errors to prevent breaking vanilla functionality
        }
    }
    
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
