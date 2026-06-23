package com.fleettools.events;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.data.PlayerDataManager;

public class PlayerJoinHandler {
    
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            // Restore fly abilities
            boolean flyEnabled = PlayerDataManager.getFlyEnabled(player);
            if (flyEnabled) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            
            // Restore god mode
            boolean godMode = PlayerDataManager.getGodMode(player);
            if (godMode) {
                player.getAbilities().invulnerable = true;
                player.onUpdateAbilities();
            }
        });
        
        // Save player's location when they disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            // Save their current location as their last known location
            PlayerDataManager.setLastLocation(player);
        });
    }
}
