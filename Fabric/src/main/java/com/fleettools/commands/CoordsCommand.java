package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class CoordsCommand {
    private static final String PERMISSION_COORDS = "fleettools.coords";

    private static final SuggestionProvider<ServerCommandSource> ONLINE_PLAYERS_SUGGESTIONS = (context, builder) -> {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            builder.suggest(player.getGameProfile().name());
        });
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("coords")
            .requires(Permissions.require(PERMISSION_COORDS, 2))
            .then(argument("player", EntityArgumentType.player())
                .suggests(ONLINE_PLAYERS_SUGGESTIONS)
                .executes(CoordsCommand::executeCoords))
        );
    }

    private static int executeCoords(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
        
        // Get player's current position
        BlockPos pos = target.getBlockPos();
        World world = ((com.fleettools.mixin.accessor.EntityAccessor)target).getWorld();
        String worldName = getWorldDisplayName(world);
        
        // Format coordinates nicely
        String coordsMessage = String.format(
            "§b%s's Location:\n§7World: §e%s\n§7X: §a%d §7Y: §a%d §7Z: §a%d",
            target.getGameProfile().name(),
            worldName,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        
        // Send to command sender
        sender.sendMessage(Text.literal(coordsMessage), false);
        
        return 1;
    }
    
    private static String getWorldDisplayName(World world) {
        String registryKey = world.getRegistryKey().getValue().toString();
        
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
