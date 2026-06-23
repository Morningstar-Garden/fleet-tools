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

public class FlyCommand {
    private static final String PERMISSION_FLY = "fleettools.fly";
    private static final String PERMISSION_FLY_OTHERS = "fleettools.fly.others";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("fly")
                .requires(Permissions.require(PERMISSION_FLY, 2))
                .executes(FlyCommand::executeFlySelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_FLY_OTHERS, 2))
                        .executes(FlyCommand::executeFlyOther)));
    }
    
    private static int executeFlySelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        boolean currentState = PlayerDataManager.getFlyEnabled(player);
        boolean newState = !currentState;
        
        setFlyMode(player, newState);
        
        String message = newState ? "§aFlight enabled." : "§cFlight disabled.";
        player.sendSystemMessage(Component.literal(message), false);
        
        return 1;
    }
    
    private static int executeFlyOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        boolean currentState = PlayerDataManager.getFlyEnabled(target);
        boolean newState = !currentState;
        
        setFlyMode(target, newState);
        
        String message = newState ? "§aFlight enabled." : "§cFlight disabled.";
        target.sendSystemMessage(Component.literal(message), false);
        
        if (context.getSource().getPlayer() != target) {
            String feedbackMessage = newState ? 
                "§aEnabled flight for " + target.getName().getString() + "." :
                "§cDisabled flight for " + target.getName().getString() + ".";
            context.getSource().sendSuccess(() -> Component.literal(feedbackMessage), true);
        }
        
        return 1;
    }
    
    private static void setFlyMode(ServerPlayer player, boolean enabled) {
        PlayerDataManager.setFlyEnabled(player, enabled);
        
        // Set the player's flight abilities
        player.getAbilities().mayfly = enabled;
        if (!enabled) {
            player.getAbilities().flying = false;
        }
        
        // Send ability updates to client
        player.onUpdateAbilities();
    }
}
