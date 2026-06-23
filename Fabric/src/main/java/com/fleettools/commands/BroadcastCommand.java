package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class BroadcastCommand {
    private static final String PERMISSION = "fleettools.broadcast";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        var node = dispatcher.register(literal("broadcast")
                .requires(Permissions.require(PERMISSION, 3))
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(BroadcastCommand::executeBroadcast)));

        // /bc alias redirects to /broadcast (same gate and logic).
        dispatcher.register(literal("bc").requires(Permissions.require(PERMISSION, 3)).redirect(node));
    }

    private static int executeBroadcast(CommandContext<CommandSourceStack> context) {
        String message = StringArgumentType.getString(context, "message");
        MinecraftServer server = context.getSource().getServer();

        Component formatted = Component.literal("§6§l[Broadcast] §r§e" + message);
        server.getPlayerList().broadcastSystemMessage(formatted, false);

        return 1;
    }
}
