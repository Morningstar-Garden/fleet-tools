package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class GamemodeCommand {
    private static final String PERMISSION_GAMEMODE = "fleettools.gamemode";
    private static final String PERMISSION_GAMEMODE_OTHERS = "fleettools.gamemode.others";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("gamemode")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .then(argument("mode", GameModeArgumentType.gameMode())
                        .executes(GamemodeCommand::executeGamemodeSelf)
                        .then(argument("player", EntityArgumentType.player())
                                .requires(Permissions.require(PERMISSION_GAMEMODE_OTHERS, 2))
                                .executes(GamemodeCommand::executeGamemodeOther))));

        // Add shortcuts for common game modes
        dispatcher.register(literal("gmc")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.CREATIVE, ctx.getSource())));

        dispatcher.register(literal("gms")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.SURVIVAL, ctx.getSource())));

        dispatcher.register(literal("gma")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.ADVENTURE,
                        ctx.getSource())));

        dispatcher.register(literal("gmsp")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.SPECTATOR,
                        ctx.getSource())));

        // Add /gm <0|1|2|3> aliases
        dispatcher.register(literal("gm")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .then(literal("0").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.SURVIVAL, ctx.getSource())))
                .then(literal("1").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.CREATIVE, ctx.getSource())))
                .then(literal("2").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(), GameMode.ADVENTURE, ctx.getSource())))
                .then(literal("3").executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrThrow(),
                        GameMode.SPECTATOR, ctx.getSource()))));
    }

    private static int executeGamemodeSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        GameMode gameMode = GameModeArgumentType.getGameMode(context, "mode");

        return changeGameMode(player, gameMode, context.getSource());
    }

    private static int executeGamemodeOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        GameMode gameMode = GameModeArgumentType.getGameMode(context, "mode");

        return changeGameMode(target, gameMode, context.getSource());
    }

    private static int changeGameMode(ServerPlayerEntity player, GameMode gameMode, ServerCommandSource source) {
        if (player.interactionManager.getGameMode() == gameMode) {
            if (source.getPlayer() == player) {
                source.sendError(Text.literal("§cYou are already in " + gameMode.name() + " mode."));
            } else {
                source.sendError(Text.literal(
                        "§c" + player.getGameProfile().name() + " is already in " + gameMode.name() + " mode."));
            }
            return 0;
        }

        player.changeGameMode(gameMode);

        if (source.getPlayer() == player) {
            player.sendMessage(Text.literal("§aYour game mode has been changed to " + gameMode.name() + "."), false);
        } else {
            player.sendMessage(Text.literal("§aYour game mode has been changed to " + gameMode.name() + "."), false);
            source.sendFeedback(() -> Text.literal(
                    "§aChanged " + player.getGameProfile().name() + "'s game mode to " + gameMode.name() + "."), true);
        }

        return 1;
    }
}
