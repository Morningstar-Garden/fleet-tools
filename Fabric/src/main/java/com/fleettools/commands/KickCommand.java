package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public class KickCommand {
    private static final String PERMISSION_KICK = "fleettools.kick";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("kick")
                .requires(Permissions.require(PERMISSION_KICK, 3))
                .then(argument("player", EntityArgument.player())
                        .executes(context -> executeKick(context, null))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(context -> executeKick(context, StringArgumentType.getString(context, "reason"))))));
    }

    private static int executeKick(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        String kickMessage = (reason == null || reason.isBlank())
                ? "§cYou have been kicked from the server."
                : "§cYou have been kicked: §f" + reason;
        target.connection.disconnect(Component.literal(kickMessage));

        String targetName = target.getName().getString();
        String feedback = (reason == null || reason.isBlank())
                ? "§aKicked " + targetName + "."
                : "§aKicked " + targetName + ": §f" + reason;
        context.getSource().sendSuccess(() -> Component.literal(feedback), true);

        return 1;
    }
}
