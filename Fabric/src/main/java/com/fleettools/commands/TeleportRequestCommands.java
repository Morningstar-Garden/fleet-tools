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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.fleettools.data.PlayerDataManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

// EssentialsX-style teleport requests with a full multi-request queue:
//   /tpa <player>      - request to teleport TO a player
//   /tphere <player>   - request that a player teleport TO you
//   /tpaccept [player] - accept a specific request, or the most recent if omitted
//   /tpdeny [player]   - deny a specific request, or the most recent if omitted
// Each recipient can hold many pending requests (one per requester); a new request
// from the same requester replaces their previous one and becomes the most recent.
public class TeleportRequestCommands {
    private static final long TIMEOUT_MS = 120_000L; // requests expire after 2 minutes

    private static final class Request {
        final UUID requesterId;
        final String requesterName;
        final boolean here; // true: recipient teleports to requester (/tphere); false: requester teleports to recipient (/tpa)
        final long time;

        Request(UUID requesterId, String requesterName, boolean here) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.here = here;
            this.time = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - time > TIMEOUT_MS;
        }
    }

    // recipient UUID -> ordered (oldest..newest) map of requester UUID -> request.
    private static final Map<UUID, LinkedHashMap<UUID, Request>> QUEUES = new java.util.HashMap<>();

    private static final SuggestionProvider<CommandSourceStack> PENDING_REQUESTERS = (context, builder) -> {
        ServerPlayer recipient = context.getSource().getPlayer();
        if (recipient != null) {
            LinkedHashMap<UUID, Request> queue = QUEUES.get(recipient.getUUID());
            if (queue != null) {
                for (Request request : queue.values()) {
                    if (!request.isExpired()) {
                        builder.suggest(request.requesterName);
                    }
                }
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("tpa")
                .requires(Permissions.require("fleettools.tpa", 0))
                .then(argument("player", EntityArgument.player())
                        .executes(context -> sendRequest(context, false))));

        dispatcher.register(literal("tphere")
                .requires(Permissions.require("fleettools.tphere", 0))
                .then(argument("player", EntityArgument.player())
                        .executes(context -> sendRequest(context, true))));

        dispatcher.register(literal("tpaccept")
                .requires(Permissions.require("fleettools.tpaccept", 0))
                .executes(context -> accept(context, null))
                .then(argument("player", StringArgumentType.word())
                        .suggests(PENDING_REQUESTERS)
                        .executes(context -> accept(context, StringArgumentType.getString(context, "player")))));

        dispatcher.register(literal("tpdeny")
                .requires(Permissions.require("fleettools.tpdeny", 0))
                .executes(context -> deny(context, null))
                .then(argument("player", StringArgumentType.word())
                        .suggests(PENDING_REQUESTERS)
                        .executes(context -> deny(context, StringArgumentType.getString(context, "player")))));
    }

    private static int sendRequest(CommandContext<CommandSourceStack> context, boolean here) throws CommandSyntaxException {
        ServerPlayer requester = context.getSource().getPlayerOrException();
        ServerPlayer recipient = EntityArgument.getPlayer(context, "player");

        if (recipient == requester) {
            requester.sendSystemMessage(Component.literal("§cYou cannot send a teleport request to yourself."), false);
            return 0;
        }

        if (!PlayerDataManager.isTpEnabled(recipient)) {
            requester.sendSystemMessage(Component.literal("§e" + recipient.getName().getString() + "§c is not accepting teleport requests."), false);
            return 0;
        }

        // Replace any existing request from this requester and move it to the most-recent slot.
        LinkedHashMap<UUID, Request> queue = QUEUES.computeIfAbsent(recipient.getUUID(), k -> new LinkedHashMap<>());
        queue.remove(requester.getUUID());
        queue.put(requester.getUUID(), new Request(requester.getUUID(), requester.getName().getString(), here));

        String prompt = here
                ? "§e" + requester.getName().getString() + "§a wants you to teleport to them."
                : "§e" + requester.getName().getString() + "§a wants to teleport to you.";
        recipient.sendSystemMessage(Component.literal(prompt + " §7Use §a/tpaccept§7 or §c/tpdeny§7 (optionally with their name)."), false);
        requester.sendSystemMessage(Component.literal("§aTeleport request sent to §e" + recipient.getName().getString() + "§a. §7(expires in 2 minutes)"), false);
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> context, String requesterName) throws CommandSyntaxException {
        ServerPlayer recipient = context.getSource().getPlayerOrException();
        Request request = (requesterName == null)
                ? takeMostRecent(recipient.getUUID())
                : takeByName(recipient.getUUID(), requesterName);

        if (request == null) {
            recipient.sendSystemMessage(Component.literal(requesterName == null
                    ? "§cYou have no pending teleport requests."
                    : "§cYou have no pending teleport request from §e" + requesterName + "§c."), false);
            return 0;
        }

        ServerPlayer requester = context.getSource().getServer().getPlayerList().getPlayer(request.requesterId);
        if (requester == null) {
            recipient.sendSystemMessage(Component.literal("§e" + request.requesterName + "§c is no longer online."), false);
            return 0;
        }

        ServerPlayer moving = request.here ? recipient : requester;
        ServerPlayer destination = request.here ? requester : recipient;

        PlayerDataManager.setLastLocation(moving); // so the moving player can /back
        moving.teleportTo(destination.level(), destination.getX(), destination.getY(), destination.getZ(),
                java.util.Set.<net.minecraft.world.entity.Relative>of(), moving.getYRot(), moving.getXRot(), false);

        moving.sendSystemMessage(Component.literal("§aTeleporting to §e" + destination.getName().getString() + "§a."), false);
        destination.sendSystemMessage(Component.literal("§e" + moving.getName().getString() + "§a has teleported to you."), false);
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> context, String requesterName) throws CommandSyntaxException {
        ServerPlayer recipient = context.getSource().getPlayerOrException();
        Request request = (requesterName == null)
                ? takeMostRecent(recipient.getUUID())
                : takeByName(recipient.getUUID(), requesterName);

        if (request == null) {
            recipient.sendSystemMessage(Component.literal(requesterName == null
                    ? "§cYou have no pending teleport requests."
                    : "§cYou have no pending teleport request from §e" + requesterName + "§c."), false);
            return 0;
        }

        recipient.sendSystemMessage(Component.literal("§cDenied teleport request from §e" + request.requesterName + "§c."), false);
        ServerPlayer requester = context.getSource().getServer().getPlayerList().getPlayer(request.requesterId);
        if (requester != null) {
            requester.sendSystemMessage(Component.literal("§e" + recipient.getName().getString() + "§c denied your teleport request."), false);
        }
        return 1;
    }

    // Removes and returns the most recent non-expired request, pruning expired ones.
    private static Request takeMostRecent(UUID recipient) {
        LinkedHashMap<UUID, Request> queue = QUEUES.get(recipient);
        if (queue == null) {
            return null;
        }
        List<UUID> keys = new ArrayList<>(queue.keySet());
        Request result = null;
        for (int i = keys.size() - 1; i >= 0; i--) {
            Request request = queue.get(keys.get(i));
            if (request.isExpired()) {
                queue.remove(keys.get(i));
                continue;
            }
            queue.remove(keys.get(i));
            result = request;
            break;
        }
        if (queue.isEmpty()) {
            QUEUES.remove(recipient);
        }
        return result;
    }

    // Removes and returns the request from the named requester, pruning expired ones.
    private static Request takeByName(UUID recipient, String name) {
        LinkedHashMap<UUID, Request> queue = QUEUES.get(recipient);
        if (queue == null) {
            return null;
        }
        Request result = null;
        Iterator<Map.Entry<UUID, Request>> it = queue.entrySet().iterator();
        while (it.hasNext()) {
            Request request = it.next().getValue();
            if (request.isExpired()) {
                it.remove();
                continue;
            }
            if (request.requesterName.equalsIgnoreCase(name)) {
                it.remove();
                result = request;
            }
        }
        if (queue.isEmpty()) {
            QUEUES.remove(recipient);
        }
        return result;
    }
}
