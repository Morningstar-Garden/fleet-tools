package com.fleettools.integration;

import com.fleettools.events.AfkManager;
import eu.pb4.placeholders.api.Placeholder;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// Registers a Text Placeholder API placeholder so AFK status can be shown by
// StyledChat / StyledPlayerList (and any other placeholder consumer).
//
//   %fleettools:afk%  ->  "[AFK] " (gray) while the player is AFK, otherwise empty.
//
// This class references eu.pb4.* types, so it must only be loaded/called when the
// Text Placeholder API is present (see FleettoolsMod's isModLoaded guard).
public final class PlaceholderIntegration {
    private PlaceholderIntegration() {
    }

    public static void register() {
        Placeholders.registerCommon(Identifier.fromNamespaceAndPath("fleettools", "afk"),
                (Placeholder.Handler<PlaceholderContext, String>) (context, argument) -> {
                    if (context.hasPlayer() && AfkManager.isAfk(context.player().getUUID())) {
                        return PlaceholderResult.value(Component.literal("[AFK] ").withStyle(ChatFormatting.GRAY));
                    }
                    return PlaceholderResult.value(Component.empty());
                });
    }
}
