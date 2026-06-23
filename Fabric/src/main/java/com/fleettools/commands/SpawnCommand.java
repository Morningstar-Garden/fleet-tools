package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;

public class SpawnCommand {
    private static final String PERMISSION_SPAWN = "fleettools.spawn";
    private static final String PERMISSION_SETSPAWN = "fleettools.setspawn";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("spawn")
                .requires(Permissions.require(PERMISSION_SPAWN, 2))
                .executes(SpawnCommand::executeSpawn));
        
        dispatcher.register(literal("setspawn")
                .requires(Permissions.require(PERMISSION_SETSPAWN, 2))
                .executes(SpawnCommand::executeSetSpawn));
    }
    
    private static int executeSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Vec3 spawnPos = PlayerDataManager.getSpawn();
        ServerLevel spawnWorld = PlayerDataManager.getSpawnWorld(player.level().getServer());
        
        if (spawnPos == null) {
            // Fall back to world spawn if no custom spawn is set
            spawnPos = new Vec3(
                spawnWorld.getRespawnData().pos().getX() + 0.5,
                spawnWorld.getRespawnData().pos().getY(),
                spawnWorld.getRespawnData().pos().getZ() + 0.5
            );
        }
        
        // Store current position for /back command
        PlayerDataManager.setLastLocation(player, player.position(), player.level());
        
        // Teleport to spawn
        player.teleportTo(spawnWorld, spawnPos.x, spawnPos.y, spawnPos.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        player.sendSystemMessage(Component.literal("§aTeleported to spawn."), false);
        
        return 1;
    }
    
    private static int executeSetSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Vec3 currentPos = player.position();
        ServerLevel currentWorld = player.level();
        
        PlayerDataManager.setSpawn(currentPos, currentWorld);
        player.sendSystemMessage(Component.literal("§aSpawn set at your current location."), false);
        
        return 1;
    }
}
