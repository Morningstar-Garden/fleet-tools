package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class TimeWeatherCommands {
    private static final String PERMISSION_TIME = "fleettools.time";
    private static final String PERMISSION_WEATHER = "fleettools.weather";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        // Time commands
        dispatcher.register(literal("day")
            .requires(Permissions.require(PERMISSION_TIME, 2))
            .executes(TimeWeatherCommands::executeDay)
        );
        
        dispatcher.register(literal("night")
            .requires(Permissions.require(PERMISSION_TIME, 2))
            .executes(TimeWeatherCommands::executeNight)
        );
        
        // Weather commands
        dispatcher.register(literal("sun")
            .requires(Permissions.require(PERMISSION_WEATHER, 2))
            .executes(TimeWeatherCommands::executeSun)
        );
        
        dispatcher.register(literal("rain")
            .requires(Permissions.require(PERMISSION_WEATHER, 2))
            .executes(TimeWeatherCommands::executeRain)
        );
        
        dispatcher.register(literal("thunderstorm")
            .requires(Permissions.require(PERMISSION_WEATHER, 2))
            .executes(TimeWeatherCommands::executeThunderstorm)
        );
    }

    // 26.2 replaced the simple ServerLevel.setDayTime/setWeather calls with a new
    // clock/timeline subsystem. Rather than reimplement it, these convenience
    // commands delegate to the vanilla /time and /weather commands. Access is
    // already gated by the Permissions.require checks above, so we run them with
    // a server-level command source so they always succeed once permitted.
    private static int runVanilla(CommandContext<CommandSourceStack> context, String command, String feedback) {
        MinecraftServer server = context.getSource().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        context.getSource().sendSuccess(() -> Component.literal(feedback), true);
        return 1;
    }

    private static int executeDay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runVanilla(context, "time set day", "§aTime set to day.");
    }

    private static int executeNight(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runVanilla(context, "time set night", "§aTime set to night.");
    }

    private static int executeSun(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runVanilla(context, "weather clear", "§aWeather set to clear/sunny.");
    }

    private static int executeRain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runVanilla(context, "weather rain", "§aWeather set to rain.");
    }

    private static int executeThunderstorm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runVanilla(context, "weather thunder", "§aWeather set to thunderstorm.");
    }
}
