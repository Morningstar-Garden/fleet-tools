package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class KeepInventoryHandler {
    
    public static void register() {
        // Register for entity death events - capture inventory before death
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                boolean keepInv = PlayerDataManager.shouldKeepInventory(player);
                if (keepInv) {
                    // Store the player's inventory in persistent storage
                    PlayerDataManager.storeInventoryOnDeath(player);
                    
                    // Temporarily enable keep inventory gamerule for this death
                    boolean wasEnabled = player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
                    if (!wasEnabled) {
                        player.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, true, player.level().getServer());
                        
                        // Schedule to restore the gamerule after a short delay
                        if (player.level().getServer() != null) {
                            player.level().getServer().execute(() -> {
                                try {
                                    Thread.sleep(50);
                                    player.level().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, false, player.level().getServer());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                        }
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
                
                // Send confirmation message
                newPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aYour inventory has been restored! §7(XP was still lost)"), false);
            }
        });
        
        // Handle player joining to restore inventory if they had one stored when they disconnected
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (PlayerDataManager.hasStoredInventory(player)) {
                // Player joined while having a stored inventory (probably disconnected while dead)
                PlayerDataManager.restoreInventoryOnRespawn(player);
                
                // Send confirmation message
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aYour inventory has been restored from before you died! §7(XP was still lost)"), false);
            }
        });
    }
}