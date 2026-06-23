package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class BanCommand {
    private static final String PERMISSION = "fleettools.ban";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers()
                .forEach(p -> builder.suggest(p.getName().getString()));
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("ban")
                .requires(Permissions.require(PERMISSION, 3))
                .then(argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> executeBan(context, null))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(context -> executeBan(context, StringArgumentType.getString(context, "reason"))))));
    }

    private static int executeBan(CommandContext<CommandSourceStack> context, String reason) {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();

        // Resolve the target's name+UUID: online player first, then the server's
        // name cache, otherwise fall back to an offline profile for the given name.
        ServerPlayer online = server.getPlayerList().getPlayerByName(playerName);
        NameAndId target = (online != null)
                ? new NameAndId(online.getGameProfile())
                : server.services().nameToIdCache().get(playerName).orElse(NameAndId.createOffline(playerName));

        UserBanList bans = server.getPlayerList().getBans();
        if (bans.isBanned(target)) {
            context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' is already banned."));
            return 0;
        }

        String banReason = (reason == null || reason.isBlank()) ? "Banned by an operator." : reason;
        bans.add(new UserBanListEntry(target, null, context.getSource().getTextName(), null, banReason));

        // Kick the player if they are currently online.
        if (online != null) {
            online.connection.disconnect(Component.literal("§cYou have been banned: §f" + banReason));
        }

        String confirmed = banReason;
        context.getSource().sendSuccess(() -> Component.literal("§aBanned " + playerName + ": §f" + confirmed), true);
        return 1;
    }
}
