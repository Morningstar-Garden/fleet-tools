package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class KeepInventoryHandler {
    
    public static void register() {
        // Register for entity death events - capture inventory before death
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                boolean keepInv = PlayerDataManager.getKeepInventory(player);
                if (keepInv) {
                    // Store the player's inventory in persistent storage
                    PlayerDataManager.storeInventoryOnDeath(player);
                    
                    // Temporarily enable keep inventory gamerule for this death
                    try {
                        net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld();
                        var gameRules = world.getGameRules();
                        var ruleClass = gameRules.getClass();
                        try {
                            var field = ruleClass.getField("KEEP_INVENTORY");
                            Object keepInvKey = field.get(null);
                            // Use reflection to call getBoolean
                            var getBooleanMethod = ruleClass.getMethod("getBoolean", keepInvKey.getClass().getSuperclass());
                            boolean wasEnabled = (Boolean) getBooleanMethod.invoke(gameRules, keepInvKey);
                            if (!wasEnabled) {
                                // Get the rule object
                                var getMethod = ruleClass.getMethod("get", keepInvKey.getClass().getSuperclass());
                                Object rule = getMethod.invoke(gameRules, keepInvKey);
                                // Set the rule to true
                                var setMethod = rule.getClass().getMethod("set", boolean.class, net.minecraft.server.MinecraftServer.class);
                                setMethod.invoke(rule, true, ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer());
                                
                                // Schedule to restore the gamerule after a short delay
                                net.minecraft.server.MinecraftServer server = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer();
                                if (server != null) {
                                    server.execute(() -> {
                                        try {
                                            Thread.sleep(50);
                                            Object ruleAgain = getMethod.invoke(gameRules, keepInvKey);
                                            setMethod.invoke(ruleAgain, false, server);
                                        } catch (Exception e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    });
                                }
                            }
                        } catch (NoSuchFieldException e) {
                            // KEEP_INVENTORY field not found, just store inventory without gamerule manipulation
                            System.err.println("Could not find KEEP_INVENTORY game rule field.");
                        }
                    } catch (Exception e) {
                        // If gamerule manipulation fails, inventory will still be restored on respawn
                        System.err.println("Failed to manipulate keep inventory gamerule: " + e.getMessage());
                    }
                }
            }
            return true; // Always allow death to proceed
        });
        
        // Handle respawn to ensure inventory is maintained
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && PlayerDataManager.hasStoredInventory(newPlayer)) {
                // Restore inventory after respawn
                PlayerDataManager.restoreInventoryOnRespawn(newPlayer);
            }
        });
        
        // Handle player joining to restore inventory if they had one stored when they disconnected
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (PlayerDataManager.hasStoredInventory(player)) {
                // Player joined while having a stored inventory (probably disconnected while dead)
                PlayerDataManager.restoreInventoryOnRespawn(player);
                
            }
        });
    }
}