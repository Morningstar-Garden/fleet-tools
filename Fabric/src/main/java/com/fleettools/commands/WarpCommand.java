package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class WarpCommand {
    private static final String PERMISSION_WARP = "fleettools.warp";
    private static final String PERMISSION_SETWARP = "fleettools.setwarp";
    private static final String PERMISSION_DELWARP = "fleettools.delwarp";

    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS = (context, builder) -> {
        PlayerDataManager.getWarps().keySet().forEach(builder::suggest);
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("warp")
                .requires(Permissions.require(PERMISSION_WARP, 2))
                .executes(WarpCommand::executeListWarps)
                .then(argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(WarpCommand::executeWarp)));

        dispatcher.register(literal("setwarp")
                .requires(Permissions.require(PERMISSION_SETWARP, 2))
                .then(argument("name", StringArgumentType.word())
                        .executes(WarpCommand::executeSetWarp)));

        dispatcher.register(literal("delwarp")
                .requires(Permissions.require(PERMISSION_DELWARP, 2))
                .then(argument("name", StringArgumentType.word())
                        .suggests(WARP_SUGGESTIONS)
                        .executes(WarpCommand::executeDelWarp)));
    }

    private static int executeListWarps(CommandContext<CommandSourceStack> context) {
        java.util.Set<String> names = PlayerDataManager.getWarps().keySet();
        if (names.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§7No warps have been set."), false);
            return 0;
        }
        int count = names.size();
        String joined = String.join(", ", new java.util.TreeSet<>(names));
        context.getSource().sendSuccess(() -> Component.literal("§eWarps (" + count + "): §f" + joined), false);
        return count;
    }

    private static int executeWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        PlayerDataManager.WarpData warp = PlayerDataManager.getWarp(name, player.level().getServer());
        if (warp == null) {
            player.sendSystemMessage(Component.literal("§cWarp '" + name + "' does not exist."), false);
            return 0;
        }
        Identifier worldId = Identifier.parse(warp.world);
        ResourceKey<net.minecraft.world.level.Level> worldKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, worldId);
        ServerLevel world = player.level().getServer().getLevel(worldKey);
        if (world == null) {
            player.sendSystemMessage(Component.literal("§cWarp world not found."), false);
            return 0;
        }
        PlayerDataManager.setLastLocation(player);
        player.teleportTo(world, warp.location.x, warp.location.y, warp.location.z, java.util.Set.<net.minecraft.world.entity.Relative>of(), warp.yaw, warp.pitch, false);
        player.sendSystemMessage(Component.literal("§aWarped to '" + name + "'."), false);
        return 1;
    }

    private static int executeSetWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        Vec3 pos = player.position();
        ServerLevel world = player.level();
        PlayerDataManager.setWarp(name, pos, world, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("§aWarp '" + name + "' set at your current location."), false);
        return 1;
    }

    private static int executeDelWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        boolean removed = PlayerDataManager.delWarp(name, player.level().getServer());
        if (removed) {
            player.sendSystemMessage(Component.literal("§aWarp '" + name + "' deleted."), false);
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cWarp '" + name + "' does not exist."), false);
            return 0;
        }
    }
}
