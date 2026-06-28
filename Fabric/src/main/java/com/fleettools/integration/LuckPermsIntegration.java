package com.fleettools.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;

import java.util.UUID;

// Reflects AFK status through LuckPerms so it flows via %luckperms:prefix% (used by
// StyledChat / StyledPlayerList and other meta consumers). On AFK we add a transient
// prefix node (in-memory only, never persisted) that prepends [AFK] to the player's
// current prefix at a high weight; on return we remove it.
//
// This class references net.luckperms.* types, so it must only be loaded/called when
// LuckPerms is present (see AfkManager's isModLoaded guard).
public final class LuckPermsIntegration {
    // High weight so the AFK-combined prefix outranks the player's normal prefix.
    private static final int AFK_WEIGHT = 99999;

    private LuckPermsIntegration() {
    }

    public static void setAfkPrefix(UUID uuid, boolean afk) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            return;
        }

        if (afk) {
            // Read the player's real prefix before adding ours (no AFK node present on a
            // not-AFK -> AFK transition). Strip its colours and lead with gray (no reset)
            // so the gray carries through the prefix text and on into the name.
            String current = user.getCachedData().getMetaData().getPrefix();
            String value = "&7[AFK] " + stripLegacyFormatting(current);
            removeAfkNodes(user);
            user.transientData().add(PrefixNode.builder(value, AFK_WEIGHT).build());
        } else {
            removeAfkNodes(user);
        }
    }

    private static void removeAfkNodes(User user) {
        user.transientData().clear(node -> node instanceof PrefixNode prefix && prefix.getPriority() == AFK_WEIGHT);
    }

    // The player's current resolved prefix (includes the transient AFK node when AFK), or "".
    public static String getPrefix(UUID uuid) {
        User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
        if (user == null) {
            return "";
        }
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }

    public static String getSuffix(UUID uuid) {
        User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
        if (user == null) {
            return "";
        }
        String suffix = user.getCachedData().getMetaData().getSuffix();
        return suffix != null ? suffix : "";
    }

    // Removes legacy color/format codes (& or section sign followed by 0-9 a-f k-o r).
    private static String stripLegacyFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("(?i)[&§][0-9A-FK-OR]", "");
    }
}
