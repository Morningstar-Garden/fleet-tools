package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class KillCommand {
    private static final String PERMISSION_KILL = "fleettools.kill";
    private static final String PERMISSION_KILL_OTHERS = "fleettools.kill.others";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("kill")
                .requires(Permissions.require(PERMISSION_KILL, 2))
                .executes(KillCommand::executeKillSelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_KILL_OTHERS, 2))
                        .executes(KillCommand::executeKillOther)));
    }

    private static int executeKillSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.kill(player.level());
        player.sendSystemMessage(Component.literal("§cYou killed yourself."), false);
        return 1;
    }

    private static int executeKillOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        target.kill(target.level());

        if (context.getSource().getPlayer() != target) {
            String targetName = target.getName().getString();
            context.getSource().sendSuccess(() -> Component.literal("§aKilled " + targetName + "."), true);
        }
        return 1;
    }
}
