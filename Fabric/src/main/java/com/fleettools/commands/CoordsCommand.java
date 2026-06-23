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
    private static final String PERMISSION_COORDS = "fleettools.coords";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            builder.suggest(player.getName().getString());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("coords")
            .requires(Permissions.require(PERMISSION_COORDS, 2))
            .then(argument("player", EntityArgument.player())
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(CoordsCommand::executeCoords))
        );
    }

    private static int executeCoords(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer sender = context.getSource().getPlayerOrException();
        
        // Get player's current position
        BlockPos pos = target.blockPosition();
        Level world = target.level();
        String worldName = getWorldDisplayName(world);
        
        // Format coordinates nicely
        String coordsMessage = String.format(
            "§b%s's Location:\n§7World: §e%s\n§7X: §a%d §7Y: §a%d §7Z: §a%d",
            target.getName().getString(),
            worldName,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        
        // Send to command sender
        sender.sendSystemMessage(Component.literal(coordsMessage), false);
        
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
