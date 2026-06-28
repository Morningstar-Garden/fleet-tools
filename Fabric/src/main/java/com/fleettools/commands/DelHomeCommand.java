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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class DelHomeCommand {
    private static final String PERMISSION_DELHOME = "fleettools.delhome";

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            PlayerDataManager.getHomeNames(player).forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("delhome")
                .requires(Permissions.require(PERMISSION_DELHOME, 2))
                .executes(context -> deleteHome(context, PlayerDataManager.DEFAULT_HOME))
                .then(argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(context -> deleteHome(context, StringArgumentType.getString(context, "name")))));
    }

    private static int deleteHome(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        name = name.toLowerCase();
        if (PlayerDataManager.removeHome(player, name)) {
            player.sendSystemMessage(Component.literal("§aDeleted home '" + name + "'."), false);
            return 1;
        }
        player.sendSystemMessage(Component.literal("§cYou don't have a home named '" + name + "'."), false);
        return 0;
    }
}
