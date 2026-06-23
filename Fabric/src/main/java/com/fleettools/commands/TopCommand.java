package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class TopCommand {
    private static final String PERMISSION_TOP = "fleettools.top";
    private static final String PERMISSION_TOP_OTHERS = "fleettools.top.others";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            builder.suggest(player.getName().getString());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("top")
            .requires(Permissions.require(PERMISSION_TOP, 2))
            .executes(TopCommand::executeTopSelf)
            .then(argument("player", EntityArgument.player())
                .requires(Permissions.require(PERMISSION_TOP_OTHERS, 2))
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(TopCommand::executeTopOther))
        );
    }

    private static int executeTopSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return teleportToTop(player, player, context);
    }

    private static int executeTopOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return teleportToTop(target, executor, context);
    }

    private static int teleportToTop(ServerPlayer target, ServerPlayer executor, CommandContext<CommandSourceStack> context) {
        ServerLevel world = target.level();
        BlockPos currentPos = target.blockPosition();
        
        // Find the highest non-air block at the player's X/Z coordinates
        int highestY = world.getHeight(Heightmap.Types.WORLD_SURFACE, currentPos.getX(), currentPos.getZ());
        
        // Check if there's actually a solid block at that position
        BlockPos topPos = new BlockPos(currentPos.getX(), highestY, currentPos.getZ());
        
        // Make sure we're not teleporting into a block by finding a safe position
        BlockPos safePos = findSafePosition(world, topPos);
        
        if (safePos == null) {
            String message = target == executor ? 
                "§cNo safe location found above you." : 
                "§cNo safe location found above " + target.getName().getString() + ".";
            executor.sendSystemMessage(Component.literal(message), false);
            return 0;
        }
        
        // Save the target's current location for /back (only for the person being teleported)
        PlayerDataManager.setLastLocation(target);
        
        // Teleport the target to the top position (slightly above the block for safety)
        target.teleportTo(world, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), target.getYRot(), target.getXRot(), false);
        
        // Send appropriate messages
        if (target == executor) {
            target.sendSystemMessage(Component.literal("§aTeleported to the top!"), false);
        } else {
            executor.sendSystemMessage(Component.literal("§aTeleported " + target.getName().getString() + " to the top."), false);
            target.sendSystemMessage(Component.literal("§aYou have been teleported to the top by " + executor.getName().getString() + "."), false);
        }
        
        return 1;
    }
    
    private static BlockPos findSafePosition(ServerLevel world, BlockPos startPos) {
        // Start from the highest point and work our way down to find a safe spot
        for (int y = startPos.getY(); y >= world.getMinY(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockPos above = checkPos.above();
            BlockPos above2 = above.above();
            
            // Check if this position has a solid block below and air above for the player
            if (!world.getBlockState(checkPos).isAir() && 
                world.getBlockState(above).isAir() && 
                world.getBlockState(above2).isAir()) {
                
                // Return the position above the solid block
                return above;
            }
        }
        
        return null; // No safe position found
    }
}
