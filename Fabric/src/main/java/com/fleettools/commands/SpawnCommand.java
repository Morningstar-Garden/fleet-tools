package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.server.command.CommandManager.literal;

public class SpawnCommand {
    private static final String PERMISSION_SPAWN = "fleettools.spawn";
    private static final String PERMISSION_SETSPAWN = "fleettools.setspawn";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("spawn")
                .requires(Permissions.require(PERMISSION_SPAWN, 2))
                .executes(SpawnCommand::executeSpawn));
        
        dispatcher.register(literal("setspawn")
                .requires(Permissions.require(PERMISSION_SETSPAWN, 2))
                .executes(SpawnCommand::executeSetSpawn));
    }
    
    private static int executeSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        Vec3d spawnPos = PlayerDataManager.getSpawn();
        ServerWorld spawnWorld = PlayerDataManager.getSpawnWorld(((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer());
        
        if (spawnPos == null) {
            // Fall back to world spawn if no custom spawn is set
            // Use sea level as default spawn Y coordinate
            BlockPos worldSpawn = new BlockPos(0, spawnWorld.getSeaLevel(), 0);
            spawnPos = new Vec3d(
                worldSpawn.getX() + 0.5,
                worldSpawn.getY(),
                worldSpawn.getZ() + 0.5
            );
        }
        
        // Store current position for /back command
        PlayerDataManager.setLastLocation(player, ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()));
        
        // Teleport to spawn
        player.teleport(spawnWorld, spawnPos.x, spawnPos.y, spawnPos.z, java.util.Set.of(), player.getYaw(), player.getPitch(), false);
        player.sendMessage(Text.literal("§aTeleported to spawn."), false);
        
        return 1;
    }
    
    private static int executeSetSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        Vec3d currentPos = ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos();
        ServerWorld currentWorld = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld());
        
        PlayerDataManager.setSpawn(currentPos, currentWorld);
        player.sendMessage(Text.literal("§aSpawn set at your current location."), false);
        
        return 1;
    }
}
