package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.server.command.CommandManager.literal;

public class KeepInventoryCommand {
    private static final String PERMISSION_KEEPINV = "fleettools.keepinv";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("keepinv")
                .requires(Permissions.require(PERMISSION_KEEPINV, 0)) // Allow all players
                .executes(KeepInventoryCommand::executeToggle));
    }
    
    private static int executeToggle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        boolean currentlyDisabled = PlayerDataManager.isKeepInventoryDisabled(player);
        boolean newState = !currentlyDisabled;
        
        PlayerDataManager.setKeepInventoryDisabled(player, newState);
        
        if (newState) {
            // Player opted out of keep inventory
            player.sendMessage(Text.literal("§cKeep inventory disabled. You will lose your items on death."), false);
        } else {
            // Player re-enabled keep inventory
            player.sendMessage(Text.literal("§aKeep inventory enabled. You will keep your items on death."), false);
        }
        
        return 1;
    }
}