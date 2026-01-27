package com.fleettools.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import com.fleettools.data.PlayerDataManager;

public class PlayerJoinHandler {
    
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            
            try {
                // Restore fly abilities
                boolean flyEnabled = PlayerDataManager.getFlyEnabled(player);
                if (flyEnabled) {
                    player.getAbilities().allowFlying = true;
                    player.sendAbilitiesUpdate();
                }
                
                // Restore god mode
                boolean godMode = PlayerDataManager.getGodMode(player);
                if (godMode) {
                    player.getAbilities().invulnerable = true;
                    player.sendAbilitiesUpdate();
                }
                
                // Safety check: Ensure any player joining is marked as off death screen
                // This handles edge cases where players disconnect while on death screen
                PlayerDataManager.markPlayerOffDeathScreen(player);
                
            } catch (Exception e) {
                System.err.println("[FleetTools] Error in player join handler: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // Save player's location when they disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            try {
                ServerPlayerEntity player = handler.getPlayer();
                if (player == null) return; // Safety check
                
                // Minimal disconnect handling to avoid conflicts with other mods
                server.execute(() -> {
                    try {
                        // Save their current location as their last known location
                        PlayerDataManager.setLastLocation(player, ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()));
                        
                        // Safety: If player disconnects while having stored inventory (death screen disconnect),
                        // ensure the data is preserved but mark them as no longer on death screen
                        if (PlayerDataManager.needsInventoryRestoration(player)) {
                            System.out.println("[FleetTools] Player " + player.getGameProfile().name() + " disconnected with pending inventory restoration");
                            // Don't clear the stored inventory, but mark them as off death screen
                            // The inventory will be restored when they rejoin
                            PlayerDataManager.markPlayerOffDeathScreen(player);
                        }
                        
                    } catch (Exception e) {
                        // Silently handle errors to avoid interfering with other mods
                        System.err.println("[FleetTools] Error in disconnect handler: " + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                // Silently handle errors to avoid interfering with other mods
                System.err.println("[FleetTools] Error in disconnect event setup: " + e.getMessage());
            }
        });
    }
}
