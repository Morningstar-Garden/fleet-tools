package com.fleettools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import com.fleettools.data.PlayerDataManager;

import static net.minecraft.commands.Commands.literal;

// Server-wide toggle for AFK protection: when enabled, AFK players take no damage
// and are ignored by mobs (enforced in ServerPlayerEntityMixin and MobTargetMixin).
// Persisted in global.json via PlayerDataManager. Defaults to enabled.
public class AfkProtectCommand {
    private static final String PERMISSION = "fleettools.afkprotect";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment) {
        dispatcher.register(literal("afkprotect")
                .requires(Permissions.require(PERMISSION, 2))
                .executes(AfkProtectCommand::toggle)
                .then(literal("on").executes(ctx -> set(ctx, true)))
                .then(literal("off").executes(ctx -> set(ctx, false)))
                .then(literal("status").executes(AfkProtectCommand::status)));
    }

    private static int toggle(CommandContext<CommandSourceStack> context) {
        return set(context, !PlayerDataManager.getAfkProtection());
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean enabled) {
        MinecraftServer server = context.getSource().getServer();
        PlayerDataManager.setAfkProtection(enabled, server);

        String message = enabled
                ? "§aAFK protection enabled. AFK players take no damage and are ignored by mobs."
                : "§cAFK protection disabled. AFK players are vulnerable again.";
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        boolean enabled = PlayerDataManager.getAfkProtection();
        String message = enabled
                ? "§aAFK protection is currently §lENABLED§r§a."
                : "§cAFK protection is currently §lDISABLED§r§c.";
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
