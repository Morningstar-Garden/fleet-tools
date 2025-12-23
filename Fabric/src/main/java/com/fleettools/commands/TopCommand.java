package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class TopCommand {
    private static final String PERMISSION_TOP = "fleettools.top";
    private static final String PERMISSION_TOP_OTHERS = "fleettools.top.others";

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            builder.suggest(player.getGameProfile().name());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("top")
            .requires(Permissions.require(PERMISSION_TOP, 2))
            .executes(TopCommand::executeTopSelf)
            .then(argument("player", EntityArgumentType.player())
                .requires(Permissions.require(PERMISSION_TOP_OTHERS, 2))
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(TopCommand::executeTopOther))
        );
    }

    private static int executeTopSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        return teleportToTop(player, player, context);
    }

    private static int executeTopOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        return teleportToTop(target, executor, context);
    }

    private static int teleportToTop(ServerPlayerEntity target, ServerPlayerEntity executor, CommandContext<ServerCommandSource> context) {
        ServerWorld world = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)target).getWorld());
        BlockPos currentPos = target.getBlockPos();
        
        // Find the highest non-air block at the player's X/Z coordinates
        int highestY = world.getTopY(Heightmap.Type.WORLD_SURFACE, currentPos.getX(), currentPos.getZ());
        
        // Check if there's actually a solid block at that position
        BlockPos topPos = new BlockPos(currentPos.getX(), highestY, currentPos.getZ());
        
        // Make sure we're not teleporting into a block by finding a safe position
        BlockPos safePos = findSafePosition(world, topPos);
        
        if (safePos == null) {
            String message = target == executor ? 
                "§cNo safe location found above you." : 
                "§cNo safe location found above " + target.getGameProfile().name() + ".";
            executor.sendMessage(Text.literal(message), false);
            return 0;
        }
        
        // Save the target's current location for /back (only for the person being teleported)
        PlayerDataManager.setLastLocation(target, ((com.fleettools.mixin.accessor.EntityPosAccessor) target).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)target).getWorld()));
        
        // Teleport the target to the top position (slightly above the block for safety)
        target.teleport(world, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, java.util.Set.of(), target.getYaw(), target.getPitch(), false);
        
        // Send appropriate messages
        if (target == executor) {
            target.sendMessage(Text.literal("§aTeleported to the top!"), false);
        } else {
            executor.sendMessage(Text.literal("§aTeleported " + target.getGameProfile().name() + " to the top."), false);
            target.sendMessage(Text.literal("§aYou have been teleported to the top by " + executor.getGameProfile().name() + "."), false);
        }
        
        return 1;
    }
    
    private static BlockPos findSafePosition(ServerWorld world, BlockPos startPos) {
        // Start from the highest point and work our way down to find a safe spot
        for (int y = startPos.getY(); y >= world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockPos above = checkPos.up();
            BlockPos above2 = above.up();
            
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
