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

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class HealCommand {
    private static final String PERMISSION_HEAL = "fleettools.heal";
    private static final String PERMISSION_HEAL_OTHERS = "fleettools.heal.others";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("heal")
                .requires(Permissions.require(PERMISSION_HEAL, 2))
                .executes(HealCommand::executeHealSelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_HEAL_OTHERS, 2))
                        .executes(HealCommand::executeHealOther)));
    }
    
    private static int executeHealSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        healPlayer(player);
        player.sendSystemMessage(Component.literal("§aYou have been healed."), false);
        
        return 1;
    }
    
    private static int executeHealOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        healPlayer(target);
        target.sendSystemMessage(Component.literal("§aYou have been healed."), false);
        
        if (context.getSource().getPlayer() != target) {
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHealed " + target.getName().getString() + "."), true);
        }
        
        return 1;
    }
    
    private static void healPlayer(ServerPlayer player) {
        // Set health to maximum
        player.setHealth(player.getMaxHealth());
        
        // Clear negative effects
        player.removeAllEffects();
        
        // Extinguish fire
        player.igniteForSeconds(0);
    }
}
