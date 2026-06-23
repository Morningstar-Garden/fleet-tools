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
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class BanIpCommand {
    private static final String PERMISSION = "fleettools.banip";
    private static final Pattern IP_PATTERN = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers()
                .forEach(p -> builder.suggest(p.getName().getString()));
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("ban-ip")
                .requires(Permissions.require(PERMISSION, 3))
                .then(argument("target", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(context -> executeBanIp(context, null))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(context -> executeBanIp(context, StringArgumentType.getString(context, "reason"))))));
    }

    private static int executeBanIp(CommandContext<CommandSourceStack> context, String reason) {
        String targetArg = StringArgumentType.getString(context, "target");
        MinecraftServer server = context.getSource().getServer();

        // A raw IP is banned directly; otherwise treat the argument as an online player's name.
        String ip;
        if (IP_PATTERN.matcher(targetArg).matches()) {
            ip = targetArg;
        } else {
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetArg);
            if (player == null) {
                context.getSource().sendFailure(Component.literal("§c'" + targetArg + "' is not a valid IP or an online player."));
                return 0;
            }
            ip = player.getIpAddress();
        }

        IpBanList ipBans = server.getPlayerList().getIpBans();
        if (ipBans.isBanned(ip)) {
            context.getSource().sendFailure(Component.literal("§cIP '" + ip + "' is already banned."));
            return 0;
        }

        String banReason = (reason == null || reason.isBlank()) ? "Banned by an operator." : reason;
        ipBans.add(new IpBanListEntry(ip, null, context.getSource().getTextName(), null, banReason));

        // Kick every online player currently connected from that IP.
        int kicked = 0;
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (player.getIpAddress().equals(ip)) {
                player.connection.disconnect(Component.literal("§cYour IP has been banned: §f" + banReason));
                kicked++;
            }
        }

        final String bannedIp = ip;
        final int kickedCount = kicked;
        context.getSource().sendSuccess(() -> Component.literal(
                "§aBanned IP " + bannedIp + (kickedCount > 0 ? " §7(kicked " + kickedCount + " player(s))" : "") + "§a: §f" + banReason), true);
        return 1;
    }
}
