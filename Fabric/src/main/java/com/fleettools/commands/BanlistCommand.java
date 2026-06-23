package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

import static net.minecraft.commands.Commands.literal;

public class BanlistCommand {
    private static final String PERMISSION = "fleettools.banlist";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("banlist")
                .requires(Permissions.require(PERMISSION, 3))
                .executes(BanlistCommand::showAll)
                .then(literal("players").executes(BanlistCommand::showPlayers))
                .then(literal("ips").executes(BanlistCommand::showIps)));
    }

    private static int showAll(CommandContext<CommandSourceStack> context) {
        showPlayers(context);
        showIps(context);
        return 1;
    }

    private static int showPlayers(CommandContext<CommandSourceStack> context) {
        PlayerList players = context.getSource().getServer().getPlayerList();
        sendList(context, "Banned players", players.getBans().getUserList());
        return 1;
    }

    private static int showIps(CommandContext<CommandSourceStack> context) {
        PlayerList players = context.getSource().getServer().getPlayerList();
        sendList(context, "Banned IPs", players.getIpBans().getUserList());
        return 1;
    }

    private static void sendList(CommandContext<CommandSourceStack> context, String title, String[] entries) {
        if (entries.length == 0) {
            context.getSource().sendSuccess(() -> Component.literal("§7" + title + ": none."), false);
        } else {
            String joined = String.join(", ", entries);
            context.getSource().sendSuccess(() -> Component.literal("§e" + title + " (" + entries.length + "): §f" + joined), false);
        }
    }
}
