package com.fleettools.commands;

import java.util.Timer;
import java.util.TimerTask;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DaylightPauseCommand {
    private static final String PERMISSION = "fleettools.daylightpause";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("daylightpause")
            .requires(Permissions.require(PERMISSION, 2))
            .then(argument("minutes", IntegerArgumentType.integer(1, 1440))
                .executes(DaylightPauseCommand::executePause))
        );
    }

    private static int executePause(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        MinecraftServer server = context.getSource().getServer();
        
        try {
            // Pause daylight cycle using direct gamerule command
            var commandManager = server.getCommandManager();
            var parseResults = commandManager.getDispatcher().parse("gamerule advance_time false", server.getCommandSource().withSilent());
            commandManager.execute(parseResults, "gamerule advance_time false");
            
            context.getSource().sendFeedback(() -> Text.literal("§aDaylight cycle paused for " + minutes + " minute(s)."), false);
            
            // Schedule resume
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // Resume daylight cycle
                    var parseResults = commandManager.getDispatcher().parse("gamerule advance_time true", server.getCommandSource().withSilent());
                    commandManager.execute(parseResults, "gamerule advance_time true");
                    server.getPlayerManager().broadcast(Text.literal("§aDaylight cycle resumed."), false);
                }
            }, minutes * 60 * 1000L);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cFailed to pause daylight cycle: " + e.getMessage()));
            return 0;
        }
    }
}
