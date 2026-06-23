package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.level.ServerLevel;

import java.util.Timer;
import java.util.TimerTask;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class DaylightPauseCommand {
    private static final String PERMISSION = "fleettools.daylightpause";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("daylightpause")
            .requires(Permissions.require(PERMISSION, 2))
            .then(argument("minutes", IntegerArgumentType.integer(1, 1440))
                .executes(DaylightPauseCommand::executePause))
        );
    }

    private static int executePause(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        MinecraftServer server = context.getSource().getServer();
        // Pause daylight cycle
        for (ServerLevel world : server.getAllLevels()) {
            world.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        }
        context.getSource().sendSuccess(() -> Component.literal("Daylight cycle paused for " + minutes + " minute(s)."), false);
        // Schedule resume
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                for (ServerLevel world : server.getAllLevels()) {
                    world.getGameRules().set(GameRules.ADVANCE_TIME, true, server);
                }
                server.getPlayerList().broadcastSystemMessage(Component.literal("Daylight cycle resumed."), false);
            }
        }, minutes * 60 * 1000L); // ms
        return 1;
    }
}
