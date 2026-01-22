package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class TimeWeatherCommands {
    private static final String PERMISSION_TIME = "fleettools.time";
    private static final String PERMISSION_WEATHER = "fleettools.weather";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
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

    private static int executeDay(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        
        // Set time to day (1000 ticks = 7:00 AM)
        world.setTimeOfDay(1000);
        
        context.getSource().sendFeedback(() -> Text.literal("§aTime set to day."), true);
        
        return 1;
    }

    private static int executeNight(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        
        // Set time to night (13000 ticks = 7:00 PM)
        world.setTimeOfDay(13000);
        
        context.getSource().sendFeedback(() -> Text.literal("§aTime set to night."), true);
        
        return 1;
    }

    private static int executeSun(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        
        // Clear weather (no rain, no thunder)
        world.setWeather(0, 0, false, false);
        
        context.getSource().sendFeedback(() -> Text.literal("§aWeather set to clear/sunny."), true);
        
        return 1;
    }

    private static int executeRain(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        
        // Set rain for 10 minutes (12000 ticks)
        world.setWeather(0, 12000, true, false);
        
        context.getSource().sendFeedback(() -> Text.literal("§aWeather set to rain."), true);
        
        return 1;
    }

    private static int executeThunderstorm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        
        // Set thunderstorm for 10 minutes (12000 ticks)
        world.setWeather(0, 12000, true, true);
        
        context.getSource().sendFeedback(() -> Text.literal("§aWeather set to thunderstorm."), true);
        
        return 1;
    }
}
