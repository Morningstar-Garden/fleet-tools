package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;

// Toggles whether the player accepts incoming teleport requests (/tpa, /tphere).
public class TpToggleCommand {
    private static final String PERMISSION = "fleettools.tptoggle";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("tptoggle")
                .requires(Permissions.require(PERMISSION, 0))
                .executes(TpToggleCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = !PlayerDataManager.isTpEnabled(player);
        PlayerDataManager.setTpEnabled(player, enabled);
        player.sendSystemMessage(Component.literal(enabled
                ? "§aYou are now accepting teleport requests."
                : "§cYou are no longer accepting teleport requests."), false);
        return 1;
    }
}
