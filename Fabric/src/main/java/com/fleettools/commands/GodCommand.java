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

public class GodCommand {
    private static final String PERMISSION_GOD = "fleettools.god";
    private static final String PERMISSION_GOD_OTHERS = "fleettools.god.others";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("god")
                .requires(Permissions.require(PERMISSION_GOD, 2))
                .executes(GodCommand::executeGodSelf)
                .then(argument("player", EntityArgumentType.player())
                        .requires(Permissions.require(PERMISSION_GOD_OTHERS, 2))
                        .executes(GodCommand::executeGodOther)));
    }
    
    private static int executeGodSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        boolean currentState = PlayerDataManager.getGodMode(player);
        boolean newState = !currentState;
        
        setGodMode(player, newState);
        
        String message = newState ? "§aGod mode enabled." : "§cGod mode disabled.";
        player.sendMessage(Text.literal(message), false);
        
        return 1;
    }
    
    private static int executeGodOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        boolean currentState = PlayerDataManager.getGodMode(target);
        boolean newState = !currentState;
        
        setGodMode(target, newState);
        
        String message = newState ? "§aGod mode enabled." : "§cGod mode disabled.";
        target.sendMessage(Text.literal(message), false);
        
        if (context.getSource().getPlayer() != target) {
            String feedbackMessage = newState ? 
                "§aEnabled god mode for " + target.getGameProfile().name() + "." :
                "§cDisabled god mode for " + target.getGameProfile().name() + ".";
            context.getSource().sendFeedback(() -> Text.literal(feedbackMessage), true);
        }
        
        return 1;
    }
    
    private static void setGodMode(ServerPlayerEntity player, boolean enabled) {
        PlayerDataManager.setGodMode(player, enabled);
        
        // Set the player's invulnerability
        player.getAbilities().invulnerable = enabled;
        
        // Send ability updates to client
        player.sendAbilitiesUpdate();
    }
}
