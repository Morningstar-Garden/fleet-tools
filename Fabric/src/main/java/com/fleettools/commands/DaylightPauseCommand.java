package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;

import java.util.Timer;
import java.util.TimerTask;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

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
        
        // Pause daylight cycle using proper GameRules API
        // Pause daylight cycle using reflection to access game rules
        try {
            for (ServerWorld world : server.getWorlds()) {
                // Get the game rules object
                var gameRules = world.getGameRules();
                // Access DO_DAYLIGHT_CYCLE using reflection since the constant name may vary in mappings
                var ruleClass = gameRules.getClass();
                try {
                    var field = ruleClass.getField("DO_DAYLIGHT_CYCLE");
                    Object ruleKey = field.get(null);  // Get the game rule key
                    var getMethod = ruleClass.getMethod("get", ruleKey.getClass().getSuperclass());
                    Object rule = getMethod.invoke(gameRules, ruleKey);
                    var setMethod = rule.getClass().getMethod("set", boolean.class, net.minecraft.server.MinecraftServer.class);
                    setMethod.invoke(rule, false, server);
                } catch (NoSuchFieldException e) {
                    // Try alternative field name
                    context.getSource().sendError(Text.literal("§cCould not find daylight cycle game rule."));
                    return 0;
                }
            }
            context.getSource().sendFeedback(() -> Text.literal("§aDaylight cycle paused for " + minutes + " minute(s)."), false);
            
            // Schedule resume
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    for (ServerWorld world : server.getWorlds()) {
                        var gameRules = world.getGameRules();
                        var ruleClass = gameRules.getClass();
                        try {
                            var field = ruleClass.getField("DO_DAYLIGHT_CYCLE");
                            Object ruleKey = field.get(null);
                            var getMethod = ruleClass.getMethod("get", ruleKey.getClass().getSuperclass());
                            Object rule = getMethod.invoke(gameRules, ruleKey);
                            var setMethod = rule.getClass().getMethod("set", boolean.class, net.minecraft.server.MinecraftServer.class);
                            setMethod.invoke(rule, true, server);
                        } catch (Exception ignored) {}
                    }
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
