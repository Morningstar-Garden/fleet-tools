package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class MuteCommand {
    private static final String PERMISSION_MUTE = "fleettools.mute";
    private static final String PERMISSION_UNMUTE = "fleettools.unmute";

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            builder.suggest(player.getGameProfile().name());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    private static final SuggestionProvider<ServerCommandSource> MUTED_PLAYERS_SUGGESTIONS = (context, builder) -> {
        // Get all muted players from PlayerDataManager
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            if (PlayerDataManager.isMuted(player)) {
                builder.suggest(player.getGameProfile().name());
            }
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // /mute command
        dispatcher.register(literal("mute")
            .requires(Permissions.require(PERMISSION_MUTE, 3))
            .then(argument("player", EntityArgumentType.player())
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(MuteCommand::executeMute))
        );

        // /unmute command
        dispatcher.register(literal("unmute")
            .requires(Permissions.require(PERMISSION_UNMUTE, 3))
            .then(argument("player", EntityArgumentType.player())
                .suggests(MUTED_PLAYERS_SUGGESTIONS)
                .executes(MuteCommand::executeUnmute))
        );
    }

    private static int executeMute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        if (PlayerDataManager.isMuted(target)) {
            context.getSource().sendError(Text.literal("§c" + target.getGameProfile().name() + " is already muted."));
            return 0;
        }
        
        PlayerDataManager.setMuted(target, true);
        context.getSource().sendFeedback(() -> Text.literal("§aMuted " + target.getGameProfile().name() + "."), true);
        target.sendMessage(Text.literal("§cYou have been muted."), false);
        
        return 1;
    }

    private static int executeUnmute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        if (!PlayerDataManager.isMuted(target)) {
            context.getSource().sendError(Text.literal("§c" + target.getGameProfile().name() + " is not muted."));
            return 0;
        }
        
        PlayerDataManager.setMuted(target, false);
        context.getSource().sendFeedback(() -> Text.literal("§aUnmuted " + target.getGameProfile().name() + "."), true);
        target.sendMessage(Text.literal("§aYou have been unmuted."), false);
        
        return 1;
    }
}
