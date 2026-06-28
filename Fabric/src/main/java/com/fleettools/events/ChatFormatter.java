package com.fleettools.events;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// EssentialsXChat-style chat formatting that keeps messages cryptographically
// signed/reportable: instead of cancelling and re-sending chat, it only decorates the
// displayed sender name (LuckPerms prefix + name + suffix) in the ChatType.Bound, while
// the signed PlayerChatMessage content is left untouched (see PlayerListMixin).
//
// Disabled when StyledChat is installed (that mod owns chat formatting), and a no-op
// without LuckPerms (no prefix/suffix to add).
public final class ChatFormatter {
    private static final boolean LUCKPERMS_PRESENT = FabricLoader.getInstance().isModLoaded("luckperms");
    private static final boolean STYLEDCHAT_PRESENT = FabricLoader.getInstance().isModLoaded("styledchat");

    private ChatFormatter() {
    }

    // The decorated sender name for chat, or null to leave the vanilla name unchanged.
    public static Component decorateName(ServerPlayer sender, Component originalName) {
        if (STYLEDCHAT_PRESENT) {
            return null; // StyledChat owns chat formatting
        }
        String prefix = LUCKPERMS_PRESENT ? com.fleettools.integration.LuckPermsIntegration.getPrefix(sender.getUUID()) : "";
        String suffix = LUCKPERMS_PRESENT ? com.fleettools.integration.LuckPermsIntegration.getSuffix(sender.getUUID()) : "";
        if (prefix.isEmpty() && suffix.isEmpty()) {
            return null;
        }
        return Component.literal(legacy(prefix)).append(originalName).append(Component.literal(legacy(suffix)));
    }

    private static String legacy(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
