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

public class FeedCommand {
    private static final String PERMISSION_FEED = "fleettools.feed";
    private static final String PERMISSION_FEED_OTHERS = "fleettools.feed.others";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("feed")
                .requires(Permissions.require(PERMISSION_FEED, 2))
                .executes(FeedCommand::executeFeedSelf)
                .then(argument("player", EntityArgumentType.player())
                        .requires(Permissions.require(PERMISSION_FEED_OTHERS, 2))
                        .executes(FeedCommand::executeFeedOther)));
    }
    
    private static int executeFeedSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        feedPlayer(player);
        player.sendMessage(Text.literal("§aYou have been fed."), false);
        
        return 1;
    }
    
    private static int executeFeedOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        
        feedPlayer(target);
        target.sendMessage(Text.literal("§aYou have been fed."), false);
        
        if (context.getSource().getPlayer() != target) {
            context.getSource().sendFeedback(() -> 
                Text.literal("§aFed " + target.getGameProfile().name() + "."), true);
        }
        
        return 1;
    }
    
    private static void feedPlayer(ServerPlayerEntity player) {
        // Set food level to maximum
        player.getHungerManager().setFoodLevel(20);
        
        // Set saturation to maximum
        player.getHungerManager().setSaturationLevel(20.0f);
    }
}
