package com.fleettools.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Central AFK state: manual /afk toggle plus auto-AFK that triggers after a period
// of no movement and clears as soon as the player moves or looks around. AFK players
// are excluded from the sleep-percentage count (see SleepStatusMixin).
public class AfkManager {
    private static final long AUTO_AFK_MS = 5 * 60 * 1000L; // auto-AFK after 5 minutes idle

    private static final Set<UUID> afk = new HashSet<>();
    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Map<UUID, double[]> lastPos = new HashMap<>(); // [x, y, z, yaw, pitch]

    // When StyledPlayerList is installed it owns the player-list names, so we let the
    // %fleettools:afk% placeholder show the AFK tag there instead of the raw tab-name
    // override (avoids conflicts / double tags).
    private static final boolean PREFIX_VANILLA_TAB =
            !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("styledplayerlist");
    private static final boolean LUCKPERMS_PRESENT =
            net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("luckperms");

    public static boolean shouldPrefixVanillaTab() {
        return PREFIX_VANILLA_TAB;
    }

    // Builds the player-list display name: LuckPerms prefix + name, grayed out with an
    // [AFK] tag while AFK. Returns null to leave the vanilla name unchanged (also when
    // StyledPlayerList is installed, since it owns the list).
    public static Component tabDisplayName(ServerPlayer player, Component base) {
        if (!PREFIX_VANILLA_TAB) {
            return null;
        }
        boolean isAfk = afk.contains(player.getUUID());
        String prefix = LUCKPERMS_PRESENT ? com.fleettools.integration.LuckPermsIntegration.getPrefix(player.getUUID()) : "";
        String legacyPrefix = prefix.replace('&', '§');

        if (isAfk) {
            // When LuckPerms is present the prefix already carries the gray [AFK] tag;
            // otherwise add it. One literal, so the gray carries through into the name.
            String head = (!legacyPrefix.isEmpty()) ? legacyPrefix : "§7[AFK] ";
            return Component.literal(head + base.getString());
        }

        if (legacyPrefix.isEmpty()) {
            return null; // no prefix and not AFK -> leave the vanilla name
        }
        return Component.literal(legacyPrefix).append(base);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AfkManager::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            lastActivity.put(player.getUUID(), System.currentTimeMillis());
            lastPos.put(player.getUUID(), snapshot(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();
            afk.remove(id);
            lastActivity.remove(id);
            lastPos.remove(id);
        });
    }

    public static boolean isAfk(UUID player) {
        return afk.contains(player);
    }

    // Manual /afk toggle.
    public static void toggle(ServerPlayer player) {
        if (afk.contains(player.getUUID())) {
            setAfk(player, false);
            lastActivity.put(player.getUUID(), System.currentTimeMillis());
        } else {
            setAfk(player, true);
        }
    }

    private static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            double[] previous = lastPos.get(id);
            double[] current = snapshot(player);
            lastPos.put(id, current);

            boolean moved = previous == null
                    || previous[0] != current[0] || previous[1] != current[1] || previous[2] != current[2]
                    || previous[3] != current[3] || previous[4] != current[4];

            if (moved) {
                lastActivity.put(id, now);
                if (afk.contains(id)) {
                    setAfk(player, false);
                }
            } else {
                Long last = lastActivity.get(id);
                if (last == null) {
                    lastActivity.put(id, now);
                } else if (!afk.contains(id) && now - last > AUTO_AFK_MS) {
                    setAfk(player, true);
                }
            }
        }
    }

    private static void setAfk(ServerPlayer player, boolean value) {
        if (value) {
            afk.add(player.getUUID());
        } else {
            afk.remove(player.getUUID());
        }

        // Reflect AFK in the LuckPerms prefix so it flows through %luckperms:prefix%.
        if (LUCKPERMS_PRESENT) {
            com.fleettools.integration.LuckPermsIntegration.setAfkPrefix(player.getUUID(), value);
        }

        MinecraftServer server = player.level().getServer();

        // Refresh the player's tab-list name for everyone (the [AFK] prefix is added by
        // ServerPlayerEntityMixin#getTabListDisplayName based on the AFK state above).
        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));

        String name = player.getName().getString();
        Component message = Component.literal(value
                ? "§7* §e" + name + "§7 is now AFK."
                : "§7* §e" + name + "§7 is no longer AFK.");
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static double[] snapshot(ServerPlayer player) {
        return new double[] { player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot() };
    }
}
