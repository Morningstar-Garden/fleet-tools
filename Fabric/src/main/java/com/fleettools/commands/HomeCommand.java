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
import net.minecraft.util.math.Vec3d;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.server.command.CommandManager.literal;

public class HomeCommand {
    private static final String PERMISSION_HOME = "fleettools.home";
    private static final String PERMISSION_SETHOME = "fleettools.sethome";
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("home")
                .requires(Permissions.require(PERMISSION_HOME, 2))
                .executes(HomeCommand::executeHome));
        
        dispatcher.register(literal("sethome")
                .requires(Permissions.require(PERMISSION_SETHOME, 2))
                .executes(HomeCommand::executeSetHome));
    }
    
    private static int executeHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        Vec3d homePos = PlayerDataManager.getHome(player);
        if (homePos == null) {
            player.sendMessage(Text.literal("§cYou don't have a home set. Use /sethome to set one."), false);
            return 0;
        }
        
        ServerWorld homeWorld = PlayerDataManager.getHomeWorld(player);
        if (homeWorld == null) {
            homeWorld = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld());
        }
        
        // Store current position for /back command
        PlayerDataManager.setLastLocation(player, ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()));
        
        // Teleport to home
        player.teleport(homeWorld, homePos.x, homePos.y, homePos.z, java.util.Set.of(), player.getYaw(), player.getPitch(), false);
        player.sendMessage(Text.literal("§aTeleported to home."), false);
        
        return 1;
    }
    
    private static int executeSetHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        
        Vec3d currentPos = ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos();
        ServerWorld currentWorld = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld());
        
        PlayerDataManager.setHome(player, currentPos, currentWorld);
        player.sendMessage(Text.literal("§aHome set at your current location."), false);
        
        return 1;
    }
}
