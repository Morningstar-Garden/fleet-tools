package com.fleettools;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.fleettools.commands.*;
import com.fleettools.events.PlayerJoinHandler;
import com.fleettools.events.TempBanHandler;
import com.fleettools.events.KeepInventoryHandler;
import com.fleettools.events.BackOnDeathHandler;
import com.fleettools.data.PlayerDataManager;

public class FleettoolsMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("[FLEET TOOLS] Initializing Fleet Tools mod - FULL VERSION");

        // Initialize player data manager and warps when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerDataManager.init(server);
            PlayerDataManager.loadWarps(server);
        });

        // Register commands that exist
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BackCommand.register(dispatcher, registryAccess, environment);
            HomeCommand.register(dispatcher, registryAccess, environment);
            SpawnCommand.register(dispatcher, registryAccess, environment);
            HealCommand.register(dispatcher, registryAccess, environment);
            GodCommand.register(dispatcher, registryAccess, environment);
            GamemodeCommand.register(dispatcher, registryAccess, environment);
            FlyCommand.register(dispatcher, registryAccess, environment);
            FeedCommand.register(dispatcher, registryAccess, environment);
            DelHomeCommand.register(dispatcher, registryAccess, environment);
            WarpCommand.register(dispatcher, registryAccess, environment);
            DaylightPauseCommand.register(dispatcher, registryAccess, environment);
            // /msg, /tell and /w are vanilla commands; remove all three so our
            // permission-gated version (and its /tell, /w aliases) takes effect
            // rather than being merged onto vanilla's nodes.
            removeCommand(dispatcher, "msg");
            removeCommand(dispatcher, "tell");
            removeCommand(dispatcher, "w");
            MsgCommand.register(dispatcher, registryAccess, environment);
            BroadcastCommand.register(dispatcher, registryAccess, environment);
            KeepInvCommand.register(dispatcher, registryAccess, environment);
            // Moderation commands
            // /ban, /ban-ip and /banlist are vanilla commands; remove them so our
            // permission-gated versions apply instead of merging onto vanilla's nodes.
            removeCommand(dispatcher, "ban");
            removeCommand(dispatcher, "ban-ip");
            removeCommand(dispatcher, "banlist");
            BanCommand.register(dispatcher, registryAccess, environment);
            BanIpCommand.register(dispatcher, registryAccess, environment);
            BanlistCommand.register(dispatcher, registryAccess, environment);
            UnbanCommand.register(dispatcher, registryAccess, environment);
            MuteCommand.register(dispatcher, registryAccess, environment);
            TempbanCommand.register(dispatcher, registryAccess, environment);
            // /kick and /kill are vanilla commands; remove the vanilla nodes first so
            // our own registrations (with fleettools permission gates) take effect
            // instead of being merged onto vanilla's node (which drops our requires()).
            removeCommand(dispatcher, "kick");
            removeCommand(dispatcher, "kill");
            KickCommand.register(dispatcher, registryAccess, environment);
            KillCommand.register(dispatcher, registryAccess, environment);
            // Utility commands
            CoordsCommand.register(dispatcher, registryAccess, environment);
            TpoCommand.register(dispatcher, registryAccess, environment);
            TopCommand.register(dispatcher, registryAccess, environment);
            TpallCommand.register(dispatcher, registryAccess, environment);
            // Time and Weather commands
            TimeWeatherCommands.register(dispatcher, registryAccess, environment);
        });

        // Register event handlers
        PlayerJoinHandler.register();
        TempBanHandler.register();
        KeepInventoryHandler.register();
        BackOnDeathHandler.register();

        System.out.println("[FLEET TOOLS] All features enabled - Commands, Events, Data Management");
    }

    /**
     * Removes a root command (e.g. a vanilla command) from the dispatcher so a mod
     * command of the same name can fully replace it instead of being merged onto it.
     * Brigadier exposes no public removal, so we clear the node from the root's
     * internal children/literals/arguments maps via reflection.
     */
    private static void removeCommand(com.mojang.brigadier.CommandDispatcher<?> dispatcher, String name) {
        com.mojang.brigadier.tree.CommandNode<?> root = dispatcher.getRoot();
        for (String fieldName : new String[] { "children", "literals", "arguments" }) {
            try {
                java.lang.reflect.Field field = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                ((java.util.Map<?, ?>) field.get(root)).remove(name);
            } catch (ReflectiveOperationException e) {
                System.err.println("[FLEET TOOLS] Could not remove vanilla command '" + name + "' (" + fieldName + "): " + e.getMessage());
            }
        }
    }
}
