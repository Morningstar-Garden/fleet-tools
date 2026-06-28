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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class MsgCommand {
    static final String PERMISSION = "fleettools.msg";

    // Last conversation partner per player, for /reply.
    private static final Map<UUID, UUID> REPLY_TARGETS = new HashMap<>();

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
            context.getSource().sendFailure(Component.literal("§cYou cannot send a message to yourself."));
            return 0;
        }

        deliver(sender, target, message);
        return 1;
    }

    // Sends a private message (actionbar) both ways and records the reply target.
    public static void deliver(ServerPlayer sender, ServerPlayer target, String message) {
        String targetMsg = "§7[§e" + sender.getName().getString() + " → You§7] §f" + message;
        String senderMsg = "§7[§eYou → " + target.getName().getString() + "§7] §f" + message;

        target.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(targetMsg)));
        sender.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(senderMsg)));

        REPLY_TARGETS.put(sender.getUUID(), target.getUUID());
        REPLY_TARGETS.put(target.getUUID(), sender.getUUID());
    }

    // The UUID this player should /reply to, or null if none.
    public static UUID getReplyTarget(UUID player) {
        return REPLY_TARGETS.get(player);
    }
}
