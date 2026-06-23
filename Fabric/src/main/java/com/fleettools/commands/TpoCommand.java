package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.fleettools.data.PlayerDataManager;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class TpoCommand {
    private static final String PERMISSION_TPO = "fleettools.tpo";

    private static final SuggestionProvider<CommandSourceStack> ALL_PLAYERS_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        
        // Add online players
        server.getPlayerList().getPlayers().forEach(player -> {
            builder.suggest(player.getName().getString());
        });
        
        // For offline players, we'll just suggest based on player data files
        File playerDataDir = server.getServerDirectory()
            .resolve("fleettools")
            .resolve("players")
            .toFile();
            
        if (playerDataDir.exists()) {
            File[] playerFiles = playerDataDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    String fileName = playerFile.getName();
                    String uuid = fileName.substring(0, fileName.length() - 5); // Remove .json
                    try {
                        java.util.UUID playerUUID = java.util.UUID.fromString(uuid);
                        NameAndId profile = server.services().nameToIdCache().get(playerUUID).orElse(null);
                        if (profile != null && profile.name() != null) {
                            // Only add if not already in online players
                            String playerName = profile.name();
                            boolean isOnline = server.getPlayerList().getPlayer(playerName) != null;
                            if (!isOnline) {
                                builder.suggest(playerName);
                            }
                        }
                    } catch (Exception ignored) {
                        // Skip invalid UUIDs or profiles
                    }
                }
            }
        }
        
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        // Register both /tpo and /tpoffline
        dispatcher.register(literal("tpo")
            .requires(Permissions.require(PERMISSION_TPO, 2))
            .then(argument("player", StringArgumentType.word())
                .suggests(ALL_PLAYERS_SUGGESTIONS)
                .executes(TpoCommand::executeTpo))
        );
        
        dispatcher.register(literal("tpoffline")
            .requires(Permissions.require(PERMISSION_TPO, 2))
            .then(argument("player", StringArgumentType.word())
                .suggests(ALL_PLAYERS_SUGGESTIONS)
                .executes(TpoCommand::executeTpo))
        );
    }

    private static int executeTpo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String targetPlayerName = StringArgumentType.getString(context, "player");
        ServerPlayer sender = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        
        // First check if the player is online
        ServerPlayer onlineTarget = server.getPlayerList().getPlayer(targetPlayerName);
        if (onlineTarget != null) {
            // Player is online, teleport to their current location
            Vec3 targetPos = onlineTarget.position();
            ServerLevel targetWorld = onlineTarget.level();
            
            // Save sender's current location for /back
            PlayerDataManager.setLastLocation(sender, sender.position(), sender.level());
            
            // Teleport to online player
            sender.teleportTo(targetWorld, targetPos.x, targetPos.y, targetPos.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), sender.getYRot(), sender.getXRot(), false);
            sender.sendSystemMessage(Component.literal("§aTeleported to §e" + targetPlayerName + "§a's current location."), false);
            
            return 1;
        }
        
        // Player is offline, try to find their last known location
        NameAndId targetProfile = null;

        // Try to find player by name in the server's name/uuid resolver cache
        targetProfile = server.services().nameToIdCache().get(targetPlayerName).orElse(null);

        if (targetProfile == null) {
            sender.sendSystemMessage(Component.literal("§cPlayer '" + targetPlayerName + "' not found."), false);
            return 0;
        }
        
        // Try to get the player's last known location from our data using UUID
        try {
            Vec3 lastLocation = PlayerDataManager.getLastLocationByUUID(targetProfile.id());
            ServerLevel lastWorld = PlayerDataManager.getLastWorldByUUID(targetProfile.id(), server);
            
            if (lastLocation == null || lastWorld == null) {
                sender.sendSystemMessage(Component.literal("§cNo last known location found for '" + targetPlayerName + "'."), false);
                return 0;
            }
            
            // Save sender's current location for /back
            PlayerDataManager.setLastLocation(sender, sender.position(), sender.level());
            
            // Teleport to offline player's last location
            sender.teleportTo(lastWorld, lastLocation.x, lastLocation.y, lastLocation.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), sender.getYRot(), sender.getXRot(), false);
            sender.sendSystemMessage(Component.literal("§aTeleported to §e" + targetPlayerName + "§a's last known location."), false);
            
            return 1;
            
        } catch (Exception e) {
            sender.sendSystemMessage(Component.literal("§cFailed to retrieve location data for '" + targetPlayerName + "'."), false);
            return 0;
        }
    }
}
