package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class MsgCommand {
    private static final String PERMISSION = "fleettools.msg";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        var msgNode = dispatcher.register(literal("msg")
            .requires(Permissions.require(PERMISSION, 2))
            .then(argument("target", EntityArgument.player())
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(MsgCommand::executeMsg)))
        );

        // Register /tell and /w as aliases that redirect to /msg (same gate and logic).
        dispatcher.register(literal("tell").requires(Permissions.require(PERMISSION, 2)).redirect(msgNode));
        dispatcher.register(literal("w").requires(Permissions.require(PERMISSION, 2)).redirect(msgNode));
    }

    private static int executeMsg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        String message = StringArgumentType.getString(context, "message");
        
        if (sender == target) {
            // Send error only to sender via direct packet (completely custom)
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("§cYou cannot send a message to yourself."));
            return 0;
        }
        
        // Format messages
        String targetMsg = "§7[§e" + sender.getName().getString() + " → You§7] §f" + message;
        String senderMsg = "§7[§eYou → " + target.getName().getString() + "§7] §f" + message;
        
        // Send messages using actionbar packets (bypasses chat completely)
        target.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
            net.minecraft.network.chat.Component.literal(targetMsg)
        ));

        sender.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
            net.minecraft.network.chat.Component.literal(senderMsg)
        ));
        
        return 1;
    }
}
