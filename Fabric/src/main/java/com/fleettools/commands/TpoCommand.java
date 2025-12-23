package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import com.fleettools.data.PlayerDataManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class TpoCommand {
    private static final String PERMISSION_TPO = "fleettools.tpo";

    private static final SuggestionProvider<ServerCommandSource> ALL_PLAYERS_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        
        // Add online players
        server.getPlayerManager().getPlayerList().forEach(player -> {
            builder.suggest(player.getGameProfile().name());
        });
        
        // For offline players, we'll just suggest based on player data files
        File playerDataDir = server.getRunDirectory()
            .resolve("fleettools")
            .resolve("players")
            .toFile();
            
        // Note: In 1.21.11, UserCache is no longer available, so we can't suggest offline players
        // Only online players will be suggested
        if (playerDataDir.exists()) {
            // Could list JSON files but without UserCache we can't resolve UUIDs to names easily
            // Skip offline player suggestions for now
        }
        
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
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

    private static int executeTpo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String targetPlayerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
        MinecraftServer server = context.getSource().getServer();
        
        // First check if the player is online
        ServerPlayerEntity onlineTarget = server.getPlayerManager().getPlayer(targetPlayerName);
        if (onlineTarget != null) {
            // Player is online, teleport to their current location
            Vec3d targetPos = ((com.fleettools.mixin.accessor.EntityPosAccessor) onlineTarget).getPos();
            ServerWorld targetWorld = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)onlineTarget).getWorld());
            
            // Save sender's current location for /back
            PlayerDataManager.setLastLocation(sender, ((com.fleettools.mixin.accessor.EntityPosAccessor) sender).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)sender).getWorld()));
            
            // Teleport to online player
            sender.teleport(targetWorld, targetPos.x, targetPos.y, targetPos.z, java.util.Set.of(), sender.getYaw(), sender.getPitch(), false);
            sender.sendMessage(Text.literal("§aTeleported to §e" + targetPlayerName + "§a's current location."), false);
            
            return 1;
        }
        
        // Player is offline - in 1.21.11, we can't easily resolve offline player UUIDs without UserCache
        // Just return an error for now
        sender.sendMessage(Text.literal("§cPlayer '" + targetPlayerName + "' is not online."), false);
        return 0;
    }
}
