package com.fleettools.events;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Staff vanish: hides a player from other players. The entity itself is hidden by
// TrackedEntityMixin and mobs ignore them via MobTargetMixin; this class owns the
// vanish state, the player-list (tab) visibility, and who is allowed to see vanished
// players.
public class VanishManager {
    private static final String PERMISSION_SEE = "fleettools.vanish.see";
    private static final Set<UUID> vanished = new HashSet<>();

    public static void register() {
        // Nothing to wire here. Tab-list hiding is handled by PlayerInfoSendMixin (which
        // filters vanished entries out of every outgoing player-info packet, including the
        // login sync), and vanish state deliberately PERSISTS across a disconnect so a
        // vanished player quits and rejoins silently (see the join/quit message mixins).
        // State is in-memory only, so a server restart clears it.
    }

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    // Ops (or anyone with fleettools.vanish.see) and other vanished players can see vanished players.
    public static boolean canSee(ServerPlayer viewer) {
        return Permissions.check(viewer, PERMISSION_SEE, 2) || vanished.contains(viewer.getUUID());
    }

    public static void setVanished(ServerPlayer player, boolean vanish) {
        if (vanish) {
            vanished.add(player.getUUID());
        } else {
            vanished.remove(player.getUUID());
        }

        MinecraftServer server = player.level().getServer();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == player || canSee(viewer)) {
                continue; // never hide from the player themselves or from staff
            }
            if (vanish) {
                viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
            } else {
                viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
            }
        }
        // The entity model is hidden/shown by TrackedEntityMixin on the next tracking tick.

        // Rebuild the player's locator-bar waypoint so a currently-shown compass marker is
        // removed on vanish (or restored on unvanish). untrackWaypoint sends REMOVE to all
        // receivers and clears connections; trackWaypoint recreates them, now filtered
        // per-receiver by WaypointVanishMixin.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.getWaypointManager().untrackWaypoint((net.minecraft.world.waypoints.WaypointTransmitter) player);
            level.getWaypointManager().trackWaypoint((net.minecraft.world.waypoints.WaypointTransmitter) player);
        }
    }
}
