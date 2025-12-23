package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import com.fleettools.data.PlayerDataManager;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class WarpCommand {
    private static final String PERMISSION_WARP = "fleettools.warp";
    private static final String PERMISSION_SETWARP = "fleettools.setwarp";
    private static final String PERMISSION_DELWARP = "fleettools.delwarp";

    private static final SuggestionProvider<ServerCommandSource> WARP_SUGGESTIONS = (context, builder) -> {
        PlayerDataManager.getWarps().keySet().forEach(builder::suggest);
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("warp")
                .requires(Permissions.require(PERMISSION_WARP, 2))
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

    private static int executeWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        PlayerDataManager.WarpData warp = PlayerDataManager.getWarp(name, ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer());
        if (warp == null) {
            player.sendMessage(Text.literal("§cWarp '" + name + "' does not exist."), false);
            return 0;
        }
        Identifier worldId = Identifier.of(warp.world);
        RegistryKey<net.minecraft.world.World> worldKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, worldId);
        ServerWorld world = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getWorld(worldKey);
        if (world == null) {
            player.sendMessage(Text.literal("§cWarp world not found."), false);
            return 0;
        }
        PlayerDataManager.setLastLocation(player, ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos(), ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()));
        player.teleport(world, warp.location.x, warp.location.y, warp.location.z, java.util.Set.of(), player.getYaw(), player.getPitch(), false);
        player.sendMessage(Text.literal("§aWarped to '" + name + "'."), false);
        return 1;
    }

    private static int executeSetWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        Vec3d pos = ((com.fleettools.mixin.accessor.EntityPosAccessor) player).getPos();
        ServerWorld world = ((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld());
        PlayerDataManager.setWarp(name, pos, world);
        player.sendMessage(Text.literal("§aWarp '" + name + "' set at your current location."), false);
        return 1;
    }

    private static int executeDelWarp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "name").toLowerCase();
        boolean removed = PlayerDataManager.delWarp(name, ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer());
        if (removed) {
            player.sendMessage(Text.literal("§aWarp '" + name + "' deleted."), false);
            return 1;
        } else {
            player.sendMessage(Text.literal("§cWarp '" + name + "' does not exist."), false);
            return 0;
        }
    }
}
