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

public class FlyCommand {
    private static final String PERMISSION_FLY = "fleettools.fly";
    private static final String PERMISSION_FLY_OTHERS = "fleettools.fly.others";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("fly")
                .requires(Permissions.require(PERMISSION_FLY, 2))
                .executes(FlyCommand::executeFlySelf)
                .then(argument("player", EntityArgumentType.player())
                        .requires(Permissions.require(PERMISSION_FLY_OTHERS, 2))
                        .executes(FlyCommand::executeFlyOther)));
    }
    
    private static int executeFlySelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        boolean currentState = PlayerDataManager.getFlyEnabled(player);
        boolean newState = !currentState;
        
        setFlyMode(player, newState);
        
        String message = newState ? "§aFlight enabled." : "§cFlight disabled.";
        player.sendMessage(Text.literal(message), false);
        
        return 1;
    }
    
    private static int executeFlyOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        boolean currentState = PlayerDataManager.getFlyEnabled(target);
        boolean newState = !currentState;
        
        setFlyMode(target, newState);
        
        String message = newState ? "§aFlight enabled." : "§cFlight disabled.";
        target.sendMessage(Text.literal(message), false);
        
        if (context.getSource().getPlayer() != target) {
            String feedbackMessage = newState ? 
                "§aEnabled flight for " + target.getGameProfile().name() + "." :
                "§cDisabled flight for " + target.getGameProfile().name() + ".";
            context.getSource().sendFeedback(() -> Text.literal(feedbackMessage), true);
        }
        
        return 1;
    }
    
    private static void setFlyMode(ServerPlayerEntity player, boolean enabled) {
        PlayerDataManager.setFlyEnabled(player, enabled);
        
        // Set the player's flight abilities
        player.getAbilities().allowFlying = enabled;
        if (!enabled) {
            player.getAbilities().flying = false;
        }
        
        // Send ability updates to client
        player.sendAbilitiesUpdate();
    }
}
