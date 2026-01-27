package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class KeepInventoryHandler {
    
    public static void register() {
        // Try to hook into damage event BEFORE death to capture backpacks
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // Only process if this damage would kill the player
                if (player.getHealth() <= damageAmount) {
                    boolean keepInv = PlayerDataManager.getKeepInventory(player);
                    // Damage event processing completed
                }
            }
            return true; // Always allow damage
        });
        
        // Register for entity death events with default priority
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                boolean keepInv = PlayerDataManager.getKeepInventory(player);
                
                if (keepInv) {
                    try {
                        // Store the player's inventory in persistent storage with damage source info
                        String deathCause = damageSource != null ? damageSource.getName() : "unknown";
                        PlayerDataManager.storeInventoryOnDeath(player, deathCause);
                        
                        System.out.println("[FleetTools] Player " + player.getGameProfile().name() + " died with keep inventory enabled (cause: " + deathCause + ")");
                        
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
                                System.err.println("[FleetTools] Could not find KEEP_INVENTORY game rule field.");
                            }
                        } catch (Exception e) {
                            // If gamerule manipulation fails, inventory will still be restored on respawn
                            System.err.println("[FleetTools] Failed to manipulate keep inventory gamerule: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("[FleetTools] Critical error during death inventory storage: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return true; // Always allow death to proceed
        });
        
        // Handle respawn to ensure inventory is maintained
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            try {
                if (!alive && PlayerDataManager.needsInventoryRestoration(newPlayer)) {
                    System.out.println("[FleetTools] Player " + newPlayer.getGameProfile().name() + " respawned, attempting inventory restoration");
                    
                    // Mark player as off death screen
                    PlayerDataManager.markPlayerOffDeathScreen(newPlayer);
                    
                    // Validate stored inventory before attempting restore
                    PlayerDataManager.validateStoredInventory(newPlayer);
                    
                    // Attempt to restore inventory with a slight delay to ensure everything is loaded
                    net.minecraft.server.MinecraftServer server = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)newPlayer).getServer();
                    if (server != null) {
                        server.execute(() -> {
                            try {
                                Thread.sleep(100); // Small delay to ensure respawn is complete
                                PlayerDataManager.restoreInventoryOnRespawn(newPlayer);
                            } catch (Exception e) {
                                System.err.println("[FleetTools] Error during delayed inventory restoration: " + e.getMessage());
                                Thread.currentThread().interrupt();
                            }
                        });
                    } else {
                        // Fallback to immediate restoration if server is null
                        PlayerDataManager.restoreInventoryOnRespawn(newPlayer);
                    }
                }
            } catch (Exception e) {
                System.err.println("[FleetTools] Error in respawn event handler: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // Handle player joining to restore inventory if they had one stored when they disconnected
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            
            try {
                // Check if player needs inventory restoration (disconnected on death screen)
                if (PlayerDataManager.needsInventoryRestoration(player)) {
                    System.out.println("[FleetTools] Player " + player.getGameProfile().name() + " joined with pending inventory restoration");
                    
                    // Validate the stored inventory data
                    PlayerDataManager.validateStoredInventory(player);
                    
                    // Multiple restoration attempts with increasing delays
                    server.execute(() -> {
                        try {
                            // First attempt - immediate
                            if (PlayerDataManager.needsInventoryRestoration(player)) {
                                System.out.println("[FleetTools] Attempting immediate inventory restoration for " + player.getGameProfile().name());
                                PlayerDataManager.restoreInventoryOnRespawn(player);
                            }
                        } catch (Exception e) {
                            System.err.println("[FleetTools] First restoration attempt failed: " + e.getMessage());
                        }
                    });
                    
                    // Second attempt with delay if first failed
                    server.execute(() -> {
                        try {
                            Thread.sleep(1000); // 1 second delay
                            if (PlayerDataManager.needsInventoryRestoration(player)) {
                                System.out.println("[FleetTools] Attempting delayed inventory restoration for " + player.getGameProfile().name());
                                PlayerDataManager.restoreInventoryOnRespawn(player);
                            }
                        } catch (Exception e) {
                            System.err.println("[FleetTools] Delayed restoration attempt failed: " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    });
                    
                    // Emergency restoration as last resort
                    server.execute(() -> {
                        try {
                            Thread.sleep(5000); // 5 second delay
                            if (PlayerDataManager.needsInventoryRestoration(player)) {
                                System.out.println("[FleetTools] Performing emergency inventory restoration for " + player.getGameProfile().name());
                                PlayerDataManager.emergencyRestoreInventory(player);
                            }
                        } catch (Exception e) {
                            System.err.println("[FleetTools] Emergency restoration failed: " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("[FleetTools] Error in join event handler for " + player.getGameProfile().name() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}