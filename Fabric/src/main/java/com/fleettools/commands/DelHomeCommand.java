package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;

public class DelHomeCommand {
    private static final String PERMISSION_DELHOME = "fleettools.delhome";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("delhome")
                .requires(Permissions.require(PERMISSION_DELHOME, 2))
                .executes(DelHomeCommand::executeDelHome));
    }

    private static int executeDelHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean removed = PlayerDataManager.removeHome(player);
        if (removed) {
            player.sendSystemMessage(Component.literal("§aYour home has been deleted."), false);
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cYou don't have a home set."), false);
            return 0;
        }
    }
}
