package com.fleettools.commands;

import com.fleettools.events.VanishManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.literal;

// Replacement for vanilla /list that omits vanished players (and adjusts the count) for
// anyone who isn't allowed to see them. Staff with fleettools.vanish.see, the console,
// and command blocks still see everyone.
public class ListCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("list")
                .executes(context -> list(context, false))
                .then(literal("uuids").executes(context -> list(context, true))));
    }

    private static int list(CommandContext<CommandSourceStack> context, boolean showUuids) {
        CommandSourceStack source = context.getSource();
        ServerPlayer viewer = source.getPlayer();
        boolean seeAll = viewer == null || VanishManager.canSee(viewer);

        List<ServerPlayer> visible = source.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> seeAll || !VanishManager.isVanished(player.getUUID()))
                .toList();

        String names = visible.stream()
                .map(player -> showUuids
                        ? player.getName().getString() + " (" + player.getUUID() + ")"
                        : player.getName().getString())
                .collect(Collectors.joining(", "));

        int max = source.getServer().getPlayerList().getMaxPlayers();
        source.sendSuccess(() -> Component.literal(
                "There are " + visible.size() + " of a max of " + max + " players online: " + names), false);
        return visible.size();
    }
}
