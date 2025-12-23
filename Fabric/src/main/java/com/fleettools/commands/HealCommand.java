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

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class HealCommand {
    private static final String PERMISSION_HEAL = "fleettools.heal";
    private static final String PERMISSION_HEAL_OTHERS = "fleettools.heal.others";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("heal")
                .requires(Permissions.require(PERMISSION_HEAL, 2))
                .executes(HealCommand::executeHealSelf)
                .then(argument("player", EntityArgumentType.player())
                        .requires(Permissions.require(PERMISSION_HEAL_OTHERS, 2))
                        .executes(HealCommand::executeHealOther)));
    }
    
    private static int executeHealSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        healPlayer(player);
        player.sendMessage(Text.literal("§aYou have been healed."), false);
        
        return 1;
    }
    
    private static int executeHealOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        healPlayer(target);
        target.sendMessage(Text.literal("§aYou have been healed."), false);
        
        if (context.getSource().getPlayer() != target) {
            context.getSource().sendFeedback(() -> 
                Text.literal("§aHealed " + target.getGameProfile().name() + "."), true);
        }
        
        return 1;
    }
    
    private static void healPlayer(ServerPlayerEntity player) {
        // Set health to maximum
        player.setHealth(player.getMaxHealth());
        
        // Clear negative effects
        player.clearStatusEffects();
        
        // Extinguish fire
        player.setOnFireFor(0);
    }
}
