package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class KeepInvCommand {
    private static final String PERMISSION_KEEPINV = "fleettools.keepinv";
    private static final String PERMISSION_KEEPINV_OTHERS = "fleettools.keepinv.others";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(literal("keepinv")
                .requires(Permissions.require(PERMISSION_KEEPINV, 0)) // Allow all players to use this command
                .executes(KeepInvCommand::executeKeepInvSelf)
                .then(literal("status")
                        .executes(KeepInvCommand::executeKeepInvStatus))
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_KEEPINV_OTHERS, 2))
                        .executes(KeepInvCommand::executeKeepInvOther)));
    }

    private static int executeKeepInvSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        boolean currentState = PlayerDataManager.getKeepInventory(player);
        boolean newState = !currentState;

        PlayerDataManager.setKeepInventory(player, newState);

        String message = newState ? "§aKeep inventory enabled. You will keep your items on death (but lose XP)."
                : "§cKeep inventory disabled. You will lose your items on death.";
        player.sendSystemMessage(Component.literal(message), false);

        return 1;
    }

    private static int executeKeepInvStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        boolean currentState = PlayerDataManager.getKeepInventory(player);
        String message = currentState ? "§aKeep inventory is currently §lENABLED§r§a. You will keep your items on death."
                : "§cKeep inventory is currently §lDISABLED§r§c. You will lose your items on death.";
        player.sendSystemMessage(Component.literal(message), false);

        return 1;
    }

    private static int executeKeepInvOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();

        boolean currentState = PlayerDataManager.getKeepInventory(target);
        boolean newState = !currentState;

        PlayerDataManager.setKeepInventory(target, newState);

        String targetMessage = newState ? "§aKeep inventory enabled. You will keep your items on death (but lose XP)."
                : "§cKeep inventory disabled. You will lose your items on death.";
        target.sendSystemMessage(Component.literal(targetMessage), false);

        String senderMessage = newState ? "§aKeep inventory enabled for " + target.getName().getString() + "."
                : "§cKeep inventory disabled for " + target.getName().getString() + ".";
        source.sendSuccess(() -> Component.literal(senderMessage), true);

        return 1;
    }
}