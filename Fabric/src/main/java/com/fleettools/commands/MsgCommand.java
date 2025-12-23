package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class MsgCommand {
    private static final String PERMISSION = "fleettools.msg";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("msg")
            .requires(Permissions.require(PERMISSION, 2))
            .then(argument("target", EntityArgumentType.player())
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(MsgCommand::executeMsg)))
        );
    }

    private static int executeMsg(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        String message = StringArgumentType.getString(context, "message");
        
        if (sender == target) {
            // Send error only to sender via direct packet (completely custom)
            context.getSource().sendError(net.minecraft.text.Text.literal("§cYou cannot send a message to yourself."));
            return 0;
        }
        
        // Format messages
        String targetMsg = "§7[§e" + sender.getGameProfile().name() + " → You§7] §f" + message;
        String senderMsg = "§7[§eYou → " + target.getGameProfile().name() + "§7] §f" + message;
        
        // Send messages using actionbar packets (bypasses chat completely)
        target.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
            net.minecraft.text.Text.literal(targetMsg)
        ));
        
        sender.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(
            net.minecraft.text.Text.literal(senderMsg)
        ));
        
        return 1;
    }
}
