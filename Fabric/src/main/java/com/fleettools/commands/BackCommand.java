package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.server.command.CommandManager.literal;

public class BackCommand {
    private static final String PERMISSION_BACK = "fleettools.back";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("back")
                .requires(Permissions.require(PERMISSION_BACK, 2))
                .executes(BackCommand::executeBack));
    }
    
    private static int executeBack(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        Vec3d lastPos = PlayerDataManager.getLastLocation(player);
        if (lastPos == null) {
            player.sendMessage(Text.literal("§cYou don't have a previous location to return to."), false);
            return 0;
        }
        
        ServerWorld lastWorld = PlayerDataManager.getLastWorld(player);
        if (lastWorld == null) {
            lastWorld = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld());
        }
        
        // Store current position as new last location
        PlayerDataManager.setLastLocation(player, ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()));
        
        // Teleport to previous location
        player.teleport(lastWorld, lastPos.x, lastPos.y, lastPos.z, java.util.Set.of(), player.getYaw(), player.getPitch(), false);
        player.sendMessage(Text.literal("Teleported to your previous location."), false);
        
        return 1;
    }
}
