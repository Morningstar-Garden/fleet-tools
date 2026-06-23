package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class MuteCommand {
    private static final String PERMISSION_MUTE = "fleettools.mute";
    private static final String PERMISSION_UNMUTE = "fleettools.unmute";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            builder.suggest(player.getName().getString());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    private static final SuggestionProvider<CommandSourceStack> MUTED_PLAYERS_SUGGESTIONS = (context, builder) -> {
        // Get all muted players from PlayerDataManager
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            if (PlayerDataManager.isMuted(player)) {
                builder.suggest(player.getName().getString());
            }
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        // /mute command
        dispatcher.register(literal("mute")
            .requires(Permissions.require(PERMISSION_MUTE, 3))
            .then(argument("player", EntityArgument.player())
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(MuteCommand::executeMute))
        );

        // /unmute command
        dispatcher.register(literal("unmute")
            .requires(Permissions.require(PERMISSION_UNMUTE, 3))
            .then(argument("player", EntityArgument.player())
                .suggests(MUTED_PLAYERS_SUGGESTIONS)
                .executes(MuteCommand::executeUnmute))
        );
    }

    private static int executeMute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (PlayerDataManager.isMuted(target)) {
            context.getSource().sendFailure(Component.literal("§c" + target.getName().getString() + " is already muted."));
            return 0;
        }
        
        PlayerDataManager.setMuted(target, true);
        context.getSource().sendSuccess(() -> Component.literal("§aMuted " + target.getName().getString() + "."), true);
        target.sendSystemMessage(Component.literal("§cYou have been muted."), false);
        
        return 1;
    }

    private static int executeUnmute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (!PlayerDataManager.isMuted(target)) {
            context.getSource().sendFailure(Component.literal("§c" + target.getName().getString() + " is not muted."));
            return 0;
        }
        
        PlayerDataManager.setMuted(target, false);
        context.getSource().sendSuccess(() -> Component.literal("§aUnmuted " + target.getName().getString() + "."), true);
        target.sendSystemMessage(Component.literal("§aYou have been unmuted."), false);
        
        return 1;
    }
}
