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

public class GodCommand {
    private static final String PERMISSION_GOD = "fleettools.god";
    private static final String PERMISSION_GOD_OTHERS = "fleettools.god.others";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("god")
                .requires(Permissions.require(PERMISSION_GOD, 2))
                .executes(GodCommand::executeGodSelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_GOD_OTHERS, 2))
                        .executes(GodCommand::executeGodOther)));
    }
    
    private static int executeGodSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        boolean currentState = PlayerDataManager.getGodMode(player);
        boolean newState = !currentState;
        
        setGodMode(player, newState);
        
        String message = newState ? "§aGod mode enabled." : "§cGod mode disabled.";
        player.sendSystemMessage(Component.literal(message), false);
        
        return 1;
    }
    
    private static int executeGodOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        boolean currentState = PlayerDataManager.getGodMode(target);
        boolean newState = !currentState;
        
        setGodMode(target, newState);
        
        String message = newState ? "§aGod mode enabled." : "§cGod mode disabled.";
        target.sendSystemMessage(Component.literal(message), false);
        
        if (context.getSource().getPlayer() != target) {
            String feedbackMessage = newState ? 
                "§aEnabled god mode for " + target.getName().getString() + "." :
                "§cDisabled god mode for " + target.getName().getString() + ".";
            context.getSource().sendSuccess(() -> Component.literal(feedbackMessage), true);
        }
        
        return 1;
    }
    
    private static void setGodMode(ServerPlayer player, boolean enabled) {
        PlayerDataManager.setGodMode(player, enabled);
        
        // Set the player's invulnerability
        player.getAbilities().invulnerable = enabled;
        
        // Send ability updates to client
        player.onUpdateAbilities();
    }
}
