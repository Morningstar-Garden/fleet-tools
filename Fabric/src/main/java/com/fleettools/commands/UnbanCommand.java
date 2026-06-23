package com.fleettools.commands;

import net.minecraft.server.players.NameAndId;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class UnbanCommand {
    private static final String PERMISSION = "fleettools.unban";

    private static final SuggestionProvider<CommandSourceStack> BANNED_PLAYERS_SUGGESTIONS = (context, builder) -> {
        UserBanList bannedPlayers = context.getSource().getServer().getPlayerList().getBans();
        for (String name : bannedPlayers.getUserList()) {
            builder.suggest(name);
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("unban")
            .requires(Permissions.require(PERMISSION, 3))
            .then(argument("player", StringArgumentType.word())
                .suggests(BANNED_PLAYERS_SUGGESTIONS)
                .executes(UnbanCommand::executeUnban))
        );
    }

    private static int executeUnban(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        
        // Try to find the player profile by name
        NameAndId profile = server.services().nameToIdCache().get(playerName).orElse(null);
        if (profile == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' not found."));
            return 0;
        }
        
        UserBanList bannedPlayers = server.getPlayerList().getBans();
        if (!bannedPlayers.isBanned(profile)) {
            context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' is not banned."));
            return 0;
        }
        
        bannedPlayers.remove(profile);
        context.getSource().sendSuccess(() -> Component.literal("§aUnbanned player '" + playerName + "'."), true);
        
        return 1;
    }
}
