package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class CoordsCommand {
    // Self: available to everyone by default (deny to restrict).
    private static final String PERMISSION_COORDS = "fleettools.coords";
    // Checking another player: operators/mods only.
    private static final String PERMISSION_COORDS_OTHERS = "fleettools.coords.others";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            builder.suggest(player.getName().getString());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("coords")
            // Default level 0 -> anyone can check their own coordinates.
            .requires(Permissions.require(PERMISSION_COORDS, 0))
            .executes(CoordsCommand::executeCoordsSelf)
            .then(argument("player", EntityArgument.player())
                // Checking others requires the elevated permission.
                .requires(Permissions.require(PERMISSION_COORDS_OTHERS, 2))
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(CoordsCommand::executeCoordsOther))
        );
    }

    private static int executeCoordsSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer self = context.getSource().getPlayerOrException();
        return sendCoords(context, self, "Your");
    }

    private static int executeCoordsOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return sendCoords(context, target, target.getName().getString() + "'s");
    }

    private static int sendCoords(CommandContext<CommandSourceStack> context, ServerPlayer target, String label) {
        BlockPos pos = target.blockPosition();
        String worldName = getWorldDisplayName(target.level());

        String coordsMessage = String.format(
            "§b%s Location:\n§7World: §e%s\n§7X: §a%d §7Y: §a%d §7Z: §a%d",
            label,
            worldName,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );

        context.getSource().sendSuccess(() -> Component.literal(coordsMessage), false);
        return 1;
    }
    
    private static String getWorldDisplayName(Level world) {
        String registryKey = world.dimension().identifier().toString();
        
        // Make world names more user-friendly
        switch (registryKey) {
            case "minecraft:overworld":
                return "Overworld";
            case "minecraft:the_nether":
                return "The Nether";
            case "minecraft:the_end":
                return "The End";
            default:
                // For custom dimensions, use the last part of the registry key
                String[] parts = registryKey.split(":");
                if (parts.length > 1) {
                    return capitalizeFirst(parts[1].replace("_", " "));
                }
                return capitalizeFirst(registryKey.replace("_", " "));
        }
    }
    
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
