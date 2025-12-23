package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class TempbanCommand {
    private static final String PERMISSION_TEMPBAN = "fleettools.tempban";
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");
    
    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            builder.suggest(player.getGameProfile().name());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    private static final SuggestionProvider<ServerCommandSource> TIME_SUGGESTIONS = (context, builder) -> {
        builder.suggest("1h");
        builder.suggest("2h");
        builder.suggest("1d");
        builder.suggest("7d");
        builder.suggest("30d");
        builder.suggest("30m");
        builder.suggest("60m");
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("tempban")
            .requires(Permissions.require(PERMISSION_TEMPBAN, 3))
            .then(argument("player", EntityArgumentType.player())
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .then(argument("time", StringArgumentType.string())
                    .suggests(TIME_SUGGESTIONS)
                    .executes(TempbanCommand::executeTempban)
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(TempbanCommand::executeTempbanWithReason))))
        );
    }

    private static int executeTempban(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return executeTempbanWithReason(context, "Temporarily banned");
    }

    private static int executeTempbanWithReason(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String reason = "Temporarily banned";
        try {
            reason = StringArgumentType.getString(context, "reason");
        } catch (IllegalArgumentException ignored) {
            // Use default reason if not provided
        }
        return executeTempbanWithReason(context, reason);
    }

    private static int executeTempbanWithReason(CommandContext<ServerCommandSource> context, String reason) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String timeString = StringArgumentType.getString(context, "time");
        
        long banDurationMs = parseTimeToMilliseconds(timeString);
        if (banDurationMs <= 0) {
            context.getSource().sendError(Text.literal("§cInvalid time format. Use format like: 1h, 30m, 1d, 7d"));
            return 0;
        }
        
        long banUntil = System.currentTimeMillis() + banDurationMs;
        
        // Store the temporary ban
        PlayerDataManager.setTempBan(target, banUntil, reason);
        
        // Disconnect the player with ban message
        String banMessage = "§cYou have been temporarily banned!\n§cReason: " + reason + "\n§cBan expires: " + formatBanExpiry(banUntil);
        target.networkHandler.disconnect(Text.literal(banMessage));
        
        // Notify administrators
        String adminMessage = "§aTemporarily banned " + target.getGameProfile().name() + " for " + timeString + " (Reason: " + reason + ")";
        context.getSource().sendFeedback(() -> Text.literal(adminMessage), true);
        
        return 1;
    }

    private static long parseTimeToMilliseconds(String timeString) {
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        if (!matcher.matches()) {
            return -1;
        }
        
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        
        switch (unit) {
            case "s": return amount * 1000L;
            case "m": return amount * 60 * 1000L;
            case "h": return amount * 60 * 60 * 1000L;
            case "d": return amount * 24 * 60 * 60 * 1000L;
            default: return -1;
        }
    }

    private static String formatBanExpiry(long expiryTime) {
        long timeLeft = expiryTime - System.currentTimeMillis();
        if (timeLeft <= 0) {
            return "Expired";
        }
        
        long days = timeLeft / (24 * 60 * 60 * 1000);
        long hours = (timeLeft % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + " day(s) " + hours + " hour(s)";
        } else if (hours > 0) {
            return hours + " hour(s) " + minutes + " minute(s)";
        } else {
            return minutes + " minute(s)";
        }
    }
}
