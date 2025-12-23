package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class KeepInvCommand {
    private static final String PERMISSION_KEEPINV = "fleettools.keepinv";
    private static final String PERMISSION_KEEPINV_OTHERS = "fleettools.keepinv.others";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("keepinv")
                .requires(Permissions.require(PERMISSION_KEEPINV, 0)) // Allow all players to use this command
                .executes(KeepInvCommand::executeKeepInvSelf)
                .then(literal("status")
                        .executes(KeepInvCommand::executeKeepInvStatus))
                .then(argument("player", EntityArgumentType.player())
                        .requires(Permissions.require(PERMISSION_KEEPINV_OTHERS, 2))
                        .executes(KeepInvCommand::executeKeepInvOther)));
    }

    private static int executeKeepInvSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        boolean currentState = PlayerDataManager.getKeepInventory(player);
        boolean newState = !currentState;

        PlayerDataManager.setKeepInventory(player, newState);

        String message = newState ? "§aKeep inventory enabled. You will keep your items on death (but lose XP)."
                : "§cKeep inventory disabled. You will lose your items on death.";
        player.sendMessage(Text.literal(message), false);

        return 1;
    }

    private static int executeKeepInvStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        boolean currentState = PlayerDataManager.getKeepInventory(player);
        String message = currentState ? "§aKeep inventory is currently §lENABLED§r§a. You will keep your items on death."
                : "§cKeep inventory is currently §lDISABLED§r§c. You will lose your items on death.";
        player.sendMessage(Text.literal(message), false);

        return 1;
    }

    private static int executeKeepInvOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        ServerCommandSource source = context.getSource();

        boolean currentState = PlayerDataManager.getKeepInventory(target);
        boolean newState = !currentState;

        PlayerDataManager.setKeepInventory(target, newState);

        String targetMessage = newState ? "§aKeep inventory enabled. You will keep your items on death (but lose XP)."
                : "§cKeep inventory disabled. You will lose your items on death.";
        target.sendMessage(Text.literal(targetMessage), false);

        String senderMessage = newState ? "§aKeep inventory enabled for " + target.getGameProfile().name() + "."
                : "§cKeep inventory disabled for " + target.getGameProfile().name() + ".";
        source.sendFeedback(() -> Text.literal(senderMessage), true);

        return 1;
    }
}