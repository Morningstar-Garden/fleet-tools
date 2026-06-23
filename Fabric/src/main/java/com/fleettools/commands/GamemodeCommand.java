package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class GamemodeCommand {
    private static final String PERMISSION_GAMEMODE = "fleettools.gamemode";
    private static final String PERMISSION_GAMEMODE_OTHERS = "fleettools.gamemode.others";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(literal("gamemode")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .then(argument("mode", GameModeArgument.gameMode())
                        .executes(GamemodeCommand::executeGamemodeSelf)
                        .then(argument("player", EntityArgument.player())
                                .requires(Permissions.require(PERMISSION_GAMEMODE_OTHERS, 2))
                                .executes(GamemodeCommand::executeGamemodeOther))));

        // Add shortcuts for common game modes
        dispatcher.register(literal("gmc")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.CREATIVE, ctx.getSource())));

        dispatcher.register(literal("gms")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.SURVIVAL, ctx.getSource())));

        dispatcher.register(literal("gma")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.ADVENTURE,
                        ctx.getSource())));

        dispatcher.register(literal("gmsp")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.SPECTATOR,
                        ctx.getSource())));

        // Add /gm <0|1|2|3> aliases
        dispatcher.register(literal("gm")
                .requires(Permissions.require(PERMISSION_GAMEMODE, 2))
                .then(literal("0").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.SURVIVAL, ctx.getSource())))
                .then(literal("1").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.CREATIVE, ctx.getSource())))
                .then(literal("2").executes(
                        ctx -> changeGameMode(ctx.getSource().getPlayerOrException(), GameType.ADVENTURE, ctx.getSource())))
                .then(literal("3").executes(ctx -> changeGameMode(ctx.getSource().getPlayerOrException(),
                        GameType.SPECTATOR, ctx.getSource()))));
    }

    private static int executeGamemodeSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        GameType gameMode = GameModeArgument.getGameMode(context, "mode");

        return changeGameMode(player, gameMode, context.getSource());
    }

    private static int executeGamemodeOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        GameType gameMode = GameModeArgument.getGameMode(context, "mode");

        return changeGameMode(target, gameMode, context.getSource());
    }

    private static int changeGameMode(ServerPlayer player, GameType gameMode, CommandSourceStack source) {
        if (player.gameMode.getGameModeForPlayer() == gameMode) {
            if (source.getPlayer() == player) {
                source.sendFailure(Component.literal("§cYou are already in " + gameMode.getName() + " mode."));
            } else {
                source.sendFailure(Component.literal(
                        "§c" + player.getName().getString() + " is already in " + gameMode.getName() + " mode."));
            }
            return 0;
        }

        player.setGameMode(gameMode);

        if (source.getPlayer() == player) {
            player.sendSystemMessage(Component.literal("§aYour game mode has been changed to " + gameMode.getName() + "."), false);
        } else {
            player.sendSystemMessage(Component.literal("§aYour game mode has been changed to " + gameMode.getName() + "."), false);
            source.sendSuccess(() -> Component.literal(
                    "§aChanged " + player.getName().getString() + "'s game mode to " + gameMode.getName() + "."), true);
        }

        return 1;
    }
}
