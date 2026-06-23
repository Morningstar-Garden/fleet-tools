package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.fleettools.data.PlayerDataManager;

import java.util.ArrayList;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class TpallCommand {
    private static final String PERMISSION = "fleettools.tpall";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("tpall")
                .requires(Permissions.require(PERMISSION, 2))
                .executes(context -> teleportAll(context, context.getSource().getPlayerOrException()))
                .then(argument("player", EntityArgument.player())
                        .executes(context -> teleportAll(context, EntityArgument.getPlayer(context, "player")))));
    }

    private static int teleportAll(CommandContext<CommandSourceStack> context, ServerPlayer destination) throws CommandSyntaxException {
        Vec3 pos = destination.position();
        ServerLevel world = destination.level();

        int count = 0;
        // Copy the list first since teleporting mutates player state during iteration.
        for (ServerPlayer player : new ArrayList<>(context.getSource().getServer().getPlayerList().getPlayers())) {
            if (player == destination) {
                continue;
            }
            // Save each player's current location so they can /back.
            PlayerDataManager.setLastLocation(player, player.position(), player.level());
            player.teleportTo(world, pos.x, pos.y, pos.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), player.getYRot(), player.getXRot(), false);
            player.sendSystemMessage(Component.literal("§aYou were teleported to " + destination.getName().getString() + "."), false);
            count++;
        }

        final int teleported = count;
        context.getSource().sendSuccess(() -> Component.literal("§aTeleported " + teleported + " player(s) to " + destination.getName().getString() + "."), true);
        return count;
    }
}
