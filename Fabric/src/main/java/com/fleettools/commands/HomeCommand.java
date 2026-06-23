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

public class HomeCommand {
    private static final String PERMISSION_HOME = "fleettools.home";
    private static final String PERMISSION_SETHOME = "fleettools.sethome";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("home")
                .requires(Permissions.require(PERMISSION_HOME, 2))
                .executes(HomeCommand::executeHome));
        
        dispatcher.register(literal("sethome")
                .requires(Permissions.require(PERMISSION_SETHOME, 2))
                .executes(HomeCommand::executeSetHome));
    }
    
    private static int executeHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Vec3 homePos = PlayerDataManager.getHome(player);
        if (homePos == null) {
            player.sendSystemMessage(Component.literal("§cYou don't have a home set. Use /sethome to set one."), false);
            return 0;
        }
        
        ServerLevel homeWorld = PlayerDataManager.getHomeWorld(player);
        if (homeWorld == null) {
            homeWorld = player.level();
        }
        
        // Store current position for /back command
        PlayerDataManager.setLastLocation(player, player.position(), player.level());
        
        // Teleport to home
        player.teleportTo(homeWorld, homePos.x, homePos.y, homePos.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
        player.sendSystemMessage(Component.literal("§aTeleported to home."), false);
        
        return 1;
    }
    
    private static int executeSetHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        Vec3 currentPos = player.position();
        ServerLevel currentWorld = player.level();
        
        PlayerDataManager.setHome(player, currentPos, currentWorld);
        player.sendSystemMessage(Component.literal("§aHome set at your current location."), false);
        
        return 1;
    }
}
