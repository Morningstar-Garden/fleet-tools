package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class MuteCommand {
    private static final String PERMISSION_MUTE = "fleettools.mute";
    private static final String PERMISSION_UNMUTE = "fleettools.unmute";

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])");

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> builder.suggest(player.getName().getString()));
        return CompletableFuture.completedFuture(builder.build());
    };

    private static final SuggestionProvider<CommandSourceStack> MUTED_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            if (PlayerDataManager.isMuted(player)) {
                builder.suggest(player.getName().getString());
            }
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTIONS = (context, builder) -> {
        for (String s : new String[] { "10m", "30m", "1h", "2h", "1d", "7d" }) {
            builder.suggest(s);
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("mute")
                .requires(Permissions.require(PERMISSION_MUTE, 3))
                .then(argument("player", EntityArgument.player())
                        .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                        .executes(context -> applyMute(context, Long.MAX_VALUE, null)) // permanent
                        .then(argument("time", StringArgumentType.string())
                                .suggests(TIME_SUGGESTIONS)
                                .executes(context -> timedMute(context, null))
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> timedMute(context, StringArgumentType.getString(context, "reason")))))));

        dispatcher.register(literal("unmute")
                .requires(Permissions.require(PERMISSION_UNMUTE, 3))
                .then(argument("player", EntityArgument.player())
                        .suggests(MUTED_PLAYERS_SUGGESTIONS)
                        .executes(MuteCommand::executeUnmute)));
    }

    private static int timedMute(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        String timeString = StringArgumentType.getString(context, "time");
        long durationMs = parseTimeToMilliseconds(timeString);
        if (durationMs <= 0) {
            context.getSource().sendFailure(Component.literal("§cInvalid time format. Use format like: 30m, 1h, 1d, 7d"));
            return 0;
        }
        return applyMute(context, System.currentTimeMillis() + durationMs, reason);
    }

    private static int applyMute(CommandContext<CommandSourceStack> context, long muteUntil, String reason) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        PlayerDataManager.mute(target, muteUntil, reason);

        String durationText = (muteUntil == Long.MAX_VALUE) ? "permanently" : "for " + formatRemaining(muteUntil);
        String reasonText = (reason == null || reason.isBlank()) ? "" : " §7(Reason: " + reason + ")";

        target.sendSystemMessage(Component.literal("§cYou have been muted " + durationText + "." + reasonText), false);
        context.getSource().sendSuccess(() -> Component.literal("§aMuted " + target.getName().getString() + " " + durationText + "." + reasonText), true);
        return 1;
    }

    private static int executeUnmute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        if (!PlayerDataManager.isMuted(target)) {
            context.getSource().sendFailure(Component.literal("§c" + target.getName().getString() + " is not muted."));
            return 0;
        }

        PlayerDataManager.unmute(target);
        context.getSource().sendSuccess(() -> Component.literal("§aUnmuted " + target.getName().getString() + "."), true);
        target.sendSystemMessage(Component.literal("§aYou have been unmuted."), false);
        return 1;
    }

    private static long parseTimeToMilliseconds(String timeString) {
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        if (!matcher.matches()) {
            return -1;
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60 * 60_000L;
            case "d" -> amount * 24 * 60 * 60_000L;
            default -> -1;
        };
    }

    private static String formatRemaining(long expiry) {
        long ms = expiry - System.currentTimeMillis();
        if (ms <= 0) {
            return "0 minutes";
        }
        long days = ms / (24 * 60 * 60_000L);
        long hours = (ms % (24 * 60 * 60_000L)) / (60 * 60_000L);
        long minutes = (ms % (60 * 60_000L)) / 60_000L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(1, minutes) + "m";
    }
}
