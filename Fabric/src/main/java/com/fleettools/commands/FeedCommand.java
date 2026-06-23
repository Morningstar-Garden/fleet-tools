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

public class FeedCommand {
    private static final String PERMISSION_FEED = "fleettools.feed";
    private static final String PERMISSION_FEED_OTHERS = "fleettools.feed.others";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("feed")
                .requires(Permissions.require(PERMISSION_FEED, 2))
                .executes(FeedCommand::executeFeedSelf)
                .then(argument("player", EntityArgument.player())
                        .requires(Permissions.require(PERMISSION_FEED_OTHERS, 2))
                        .executes(FeedCommand::executeFeedOther)));
    }
    
    private static int executeFeedSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        feedPlayer(player);
        player.sendSystemMessage(Component.literal("§aYou have been fed."), false);
        
        return 1;
    }
    
    private static int executeFeedOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        feedPlayer(target);
        target.sendSystemMessage(Component.literal("§aYou have been fed."), false);
        
        if (context.getSource().getPlayer() != target) {
            context.getSource().sendSuccess(() -> 
                Component.literal("§aFed " + target.getName().getString() + "."), true);
        }
        
        return 1;
    }
    
    private static void feedPlayer(ServerPlayer player) {
        // Set food level to maximum
        player.getFoodData().setFoodLevel(20);
        
        // Set saturation to maximum
        player.getFoodData().setSaturation(20.0f);
    }
}
