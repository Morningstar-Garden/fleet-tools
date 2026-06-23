package com.fleettools.events;

import com.fleettools.data.PlayerDataManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class TempBanHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Check if player is temporarily banned on join
            if (PlayerDataManager.isTempBanned(handler.getPlayer())) {
                long expiryTime = PlayerDataManager.getTempBanExpiry(handler.getPlayer());
                String reason = PlayerDataManager.getTempBanReason(handler.getPlayer());
                
                long timeLeft = expiryTime - System.currentTimeMillis();
                String timeLeftFormatted = formatTimeLeft(timeLeft);
                
                String banMessage = "§cYou are temporarily banned!\n" +
                                   "§cReason: " + reason + "\n" +
                                   "§cTime remaining: " + timeLeftFormatted;
                
                handler.disconnect(Component.literal(banMessage));
            }
        });
    }
    
    private static String formatTimeLeft(long timeLeft) {
        if (timeLeft <= 0) {
            return "Expired";
        }
        
        long days = timeLeft / (24 * 60 * 60 * 1000);
        long hours = (timeLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + " day(s) " + hours + " hour(s)";
        } else if (hours > 0) {
            return hours + " hour(s) " + minutes + " minute(s)";
        } else {
            return minutes + " minute(s)";
        }
    }
}
