package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;
import com.fleettools.data.PlayerDataManager.HomeData;

import java.util.Set;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class HomeCommand {
    private static final String PERMISSION_HOME = "fleettools.home";
    private static final String PERMISSION_SETHOME = "fleettools.sethome";
    private static final String PERMISSION_HOMES_UNLIMITED = "fleettools.homes.unlimited";
    private static final int DEFAULT_HOME_LIMIT = 3;

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            PlayerDataManager.getHomeNames(player).forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("home")
                .requires(Permissions.require(PERMISSION_HOME, 2))
                .executes(context -> teleportHome(context, null))
                .then(argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(context -> teleportHome(context, StringArgumentType.getString(context, "name")))));

        dispatcher.register(literal("sethome")
                .requires(Permissions.require(PERMISSION_SETHOME, 2))
                .executes(context -> setHome(context, PlayerDataManager.DEFAULT_HOME))
                .then(argument("name", StringArgumentType.word())
                        .executes(context -> setHome(context, StringArgumentType.getString(context, "name")))));

        dispatcher.register(literal("homes")
                .requires(Permissions.require(PERMISSION_HOME, 2))
                .executes(HomeCommand::listHomes));
    }

    private static int teleportHome(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Set<String> names = PlayerDataManager.getHomeNames(player);
        if (names.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou don't have any homes set. Use /sethome to set one."), false);
            return 0;
        }

        String target;
        if (name == null) {
            if (names.contains(PlayerDataManager.DEFAULT_HOME)) {
                target = PlayerDataManager.DEFAULT_HOME;
            } else if (names.size() == 1) {
                target = names.iterator().next();
            } else {
                player.sendSystemMessage(Component.literal("§eYour homes: §f" + String.join(", ", names) + "§7. Use §f/home <name>§7."), false);
                return 0;
            }
        } else {
            target = name.toLowerCase();
        }

        HomeData home = PlayerDataManager.getHome(player, target);
        if (home == null) {
            player.sendSystemMessage(Component.literal("§cNo home named '" + target + "'. §7Your homes: §f" + String.join(", ", names)), false);
            return 0;
        }

        ServerLevel world = PlayerDataManager.resolveWorld(player, home.world);
        if (world == null) {
            world = player.level();
        }

        PlayerDataManager.setLastLocation(player);
        player.teleportTo(world, home.location.x, home.location.y, home.location.z,
                Set.<net.minecraft.world.entity.Relative>of(), home.yaw, home.pitch, false);
        player.sendSystemMessage(Component.literal("§aTeleported to home '" + target + "'."), false);
        return 1;
    }

    private static int setHome(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        name = name.toLowerCase();

        boolean replacing = PlayerDataManager.getHome(player, name) != null;
        if (!replacing) {
            int limit = Permissions.check(player, PERMISSION_HOMES_UNLIMITED, false) ? Integer.MAX_VALUE : DEFAULT_HOME_LIMIT;
            if (PlayerDataManager.getHomeCount(player) >= limit) {
                player.sendSystemMessage(Component.literal("§cYou've reached your home limit (" + limit + "). Delete one with /delhome, or reuse an existing name."), false);
                return 0;
            }
        }

        PlayerDataManager.setHome(player, name, player.position(), player.level(), player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("§aHome '" + name + "' set at your current location."), false);
        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Set<String> names = PlayerDataManager.getHomeNames(player);
        if (names.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7You don't have any homes set."), false);
            return 0;
        }
        player.sendSystemMessage(Component.literal("§eHomes (" + names.size() + "): §f" + String.join(", ", names)), false);
        return names.size();
    }
}
