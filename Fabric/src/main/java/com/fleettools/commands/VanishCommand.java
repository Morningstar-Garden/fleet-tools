package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.events.VanishManager;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class VanishCommand {
    private static final String PERMISSION = "fleettools.vanish";
    private static final String PERMISSION_OTHERS = "fleettools.vanish.others";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        var node = dispatcher.register(literal("vanish")
                .requires(Permissions.require(PERMISSION, 2))
                .executes(VanishCommand::vanishSelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_OTHERS, 2))
                        .executes(VanishCommand::vanishOther)));

        dispatcher.register(literal("v").requires(Permissions.require(PERMISSION, 2)).redirect(node));
    }

    private static int vanishSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean nowVanished = !VanishManager.isVanished(player.getUUID());
        VanishManager.setVanished(player, nowVanished);
        player.sendSystemMessage(Component.literal(nowVanished
                ? "§aYou are now vanished."
                : "§eYou are no longer vanished."), false);
        return 1;
    }

    private static int vanishOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        boolean nowVanished = !VanishManager.isVanished(target.getUUID());
        VanishManager.setVanished(target, nowVanished);

        target.sendSystemMessage(Component.literal(nowVanished
                ? "§aYou have been vanished."
                : "§eYou are no longer vanished."), false);
        context.getSource().sendSuccess(() -> Component.literal(
                (nowVanished ? "§aVanished " : "§eUnvanished ") + target.getName().getString() + "."), true);
        return 1;
    }
}
