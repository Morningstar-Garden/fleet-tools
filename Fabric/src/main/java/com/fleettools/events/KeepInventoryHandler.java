package com.fleettools.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import com.fleettools.data.PlayerDataManager;

public class KeepInventoryHandler {
    
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KeepInventoryHandler::onEntityDeath);
    }
    
    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        // Only handle player deaths
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        
        ServerWorld world = (ServerWorld) ((com.fleettools.mixin.accessor.EntityAccessor) entity).getWorld();
        Vec3d deathPos = ((com.fleettools.mixin.accessor.EntityPosAccessor) entity).getPos();
        
        // ALWAYS drop experience for all players (regardless of keep inventory setting)
        if (player.experienceLevel > 0 || player.totalExperience > 0) {
            // Calculate total XP and drop as experience orbs
            int totalXp = player.totalExperience;
            if (totalXp > 0) {
                // Drop experience orbs at death location
                while (totalXp > 0) {
                    int orbValue = Math.min(totalXp, 1000); // Limit orb size
                    net.minecraft.entity.ExperienceOrbEntity.spawn(world, deathPos, orbValue);
                    totalXp -= orbValue;
                }
            }
            // Clear player's experience
            player.experienceLevel = 0;
            player.experienceProgress = 0.0f;
            player.totalExperience = 0;
        }
        
        // Check if this player has opted out of keep inventory
        boolean keepInventoryDisabled = PlayerDataManager.isKeepInventoryDisabled(player);
        
        if (!keepInventoryDisabled) {
            // Player wants to keep inventory - let vanilla handle it (but XP was already dropped above)
            return;
        }
        
        // Player has opted out of keep inventory - we need to drop their items
        // Since the gamerule keepInventory is true, vanilla won't drop items, so we need to do it manually
        
        // Drop all items from player inventory including modded inventory slots (like Nemo's Backpacks)
        // We iterate through an extended range to catch any modded inventory extensions
        for (int i = 0; i < 200; i++) { // Extended range to catch backpack slots
            try {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    // Create a copy and clear the slot
                    ItemStack dropStack = stack.copy();
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                    
                    // Drop the item at death location
                    player.dropStack(world, dropStack, 0.0f);
                }
            } catch (IndexOutOfBoundsException e) {
                // We've reached the end of available inventory slots
                break;
            } catch (Exception e) {
                // Continue if there's any other error accessing a slot
                continue;
            }
        }
    }
}