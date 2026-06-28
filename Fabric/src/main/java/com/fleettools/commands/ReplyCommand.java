package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class ReplyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        var node = dispatcher.register(literal("reply")
                .requires(Permissions.require(MsgCommand.PERMISSION, 2))
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(ReplyCommand::executeReply)));

        // /r alias.
        dispatcher.register(literal("r").requires(Permissions.require(MsgCommand.PERMISSION, 2)).redirect(node));
    }

    private static int executeReply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();

        UUID targetId = MsgCommand.getReplyTarget(sender.getUUID());
        if (targetId == null) {
            sender.sendSystemMessage(Component.literal("§cYou have no one to reply to."), false);
            return 0;
        }

        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayer(targetId);
        if (target == null) {
            sender.sendSystemMessage(Component.literal("§cThat player is no longer online."), false);
            return 0;
        }

        MsgCommand.deliver(sender, target, StringArgumentType.getString(context, "message"));
        return 1;
    }
}
