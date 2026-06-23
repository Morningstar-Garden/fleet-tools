
package com.fleettools.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class PlayerDataManager {
    // --- Warp System ---
    public static class WarpData {
        public Vec3 location;
        public String world;

        public WarpData() {
        }

        public WarpData(Vec3 location, String world) {
            this.location = location;
            this.world = world;
        }
    }

    private static final String WARPS_FILE = "warps.json";
    private static final java.lang.reflect.Type WARP_MAP_TYPE = new com.google.gson.reflect.TypeToken<Map<String, WarpData>>() {
    }.getType();
    private static Map<String, WarpData> warps = new HashMap<>();

    public static void loadWarps(MinecraftServer server) {
        try {
            Path warpsFile = server.getServerDirectory().resolve(DATA_FOLDER).resolve(WARPS_FILE);
            if (Files.exists(warpsFile)) {
                String json = Files.readString(warpsFile);
                Map<String, WarpData> loaded = GSON.fromJson(json, WARP_MAP_TYPE);
                if (loaded != null)
                    warps = loaded;
            }
        } catch (IOException e) {
            System.err.println("Failed to load warps: " + e.getMessage());
        }
    }

    public static void saveWarps(MinecraftServer server) {
        try {
            Path warpsFile = server.getServerDirectory().resolve(DATA_FOLDER).resolve(WARPS_FILE);
            String json = GSON.toJson(warps, WARP_MAP_TYPE);
            Files.writeString(warpsFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save warps: " + e.getMessage());
        }
    }

    public static Map<String, WarpData> getWarps() {
        return warps;
    }

    public static void setWarp(String name, Vec3 location, ServerLevel world) {
        warps.put(name, new WarpData(location, world.dimension().identifier().toString()));
        saveWarps(world.getServer());
    }

    public static boolean delWarp(String name, MinecraftServer server) {
        boolean removed = warps.remove(name) != null;
        saveWarps(server);
        return removed;
    }

    public static WarpData getWarp(String name, MinecraftServer server) {
        return warps.get(name);
    }

    // Removes the player's home and returns true if a home was removed
    public static boolean removeHome(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        if (data.homeLocation != null) {
            data.homeLocation = null;
            data.homeWorld = null;
            savePlayerData(player);
            return true;
        }
        return false;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FOLDER = "fleettools";
    private static final String PLAYERS_FOLDER = "players";
    private static final String GLOBAL_DATA_FILE = "global.json";

    private static final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private static GlobalData globalData;
    private static MinecraftServer serverInstance;

    public static class PlayerData {
        public Vec3 homeLocation;
        public String homeWorld;
        public Vec3 lastLocation;
        public String lastWorld;
        public boolean godMode = false;
        public boolean flyEnabled = false;
        public boolean muted = false;
        public boolean keepInventory = true; // Opt-out keep inventory - enabled by default
        public long tempBanUntil = 0; // Timestamp when temp ban expires (0 = not banned)
        public String tempBanReason = "";
        
        // Stored inventory data for keep inventory feature
        public transient StoredInventoryData storedInventory = null;

        public PlayerData() {
        }
    }
    
    public static class StoredInventoryData {
        // 1.21.x flattened the player inventory into a single indexed container
        // (getContainerSize / getItem / setItem), so we snapshot the whole thing
        // generically instead of tracking main/armor/offhand separately.
        public transient ItemStack[] contents;
        public int selectedSlot;
        public long deathTime;

        public StoredInventoryData() {
        }

        public StoredInventoryData(ServerPlayer player) {
            var inventory = player.getInventory();
            int size = inventory.getContainerSize();
            this.contents = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                this.contents[i] = inventory.getItem(i).copy();
            }
            this.selectedSlot = inventory.getSelectedSlot();
            this.deathTime = System.currentTimeMillis();
        }

        public void restore(ServerPlayer player) {
            var inventory = player.getInventory();
            inventory.clearContent();
            if (this.contents != null) {
                int size = Math.min(this.contents.length, inventory.getContainerSize());
                for (int i = 0; i < size; i++) {
                    inventory.setItem(i, this.contents[i].copy());
                }
            }
            inventory.setSelectedSlot(this.selectedSlot);

            // Push the changes to the client.
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(inventory);
            player.onUpdateAbilities();
        }
    }

    public static class GlobalData {
        public Vec3 spawnLocation;
        public String spawnWorld;

        public GlobalData() {
        }
    }

    public static void init(MinecraftServer server) {
        serverInstance = server; // Store server instance for later use
        try {
            Path dataDir = server.getServerDirectory().resolve(DATA_FOLDER);
            Path playersDir = dataDir.resolve(PLAYERS_FOLDER);

            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            if (!Files.exists(playersDir)) {
                Files.createDirectories(playersDir);
            }

            // Load global data
            loadGlobalData(server);

        } catch (IOException e) {
            System.err.println("Failed to initialize FleetTools data manager: " + e.getMessage());
        }
    }

    private static void loadGlobalData(MinecraftServer server) {
        try {
            Path globalFile = server.getServerDirectory().resolve(DATA_FOLDER).resolve(GLOBAL_DATA_FILE);
            if (Files.exists(globalFile)) {
                String json = Files.readString(globalFile);
                globalData = GSON.fromJson(json, GlobalData.class);
            } else {
                globalData = new GlobalData();
            }
        } catch (IOException e) {
            System.err.println("Failed to load global data: " + e.getMessage());
            globalData = new GlobalData();
        }
    }

    private static void saveGlobalData(MinecraftServer server) {
        try {
            Path globalFile = server.getServerDirectory().resolve(DATA_FOLDER).resolve(GLOBAL_DATA_FILE);
            String json = GSON.toJson(globalData);
            Files.writeString(globalFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save global data: " + e.getMessage());
        }
    }

    public static PlayerData getPlayerData(ServerPlayer player) {
        UUID uuid = player.getUUID();

        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        // Load from file
        try {
            Path playerFile = player.level().getServer().getServerDirectory()
                    .resolve(DATA_FOLDER)
                    .resolve(PLAYERS_FOLDER)
                    .resolve(uuid.toString() + ".json");

            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                PlayerData data = GSON.fromJson(json, PlayerData.class);
                playerDataCache.put(uuid, data);
                return data;
            }
        } catch (IOException e) {
            System.err
                    .println("Failed to load player data for " + player.getName().getString() + ": " + e.getMessage());
        }

        // Create new data
        PlayerData data = new PlayerData();
        playerDataCache.put(uuid, data);
        return data;
    }

    public static void savePlayerData(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerData data = playerDataCache.get(uuid);

        if (data == null)
            return;

        try {
            Path playerFile = player.level().getServer().getServerDirectory()
                    .resolve(DATA_FOLDER)
                    .resolve(PLAYERS_FOLDER)
                    .resolve(uuid.toString() + ".json");

            String json = GSON.toJson(data);
            Files.writeString(playerFile, json);
        } catch (IOException e) {
            System.err
                    .println("Failed to save player data for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    // Home methods
    public static Vec3 getHome(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.homeLocation;
    }

    public static ServerLevel getHomeWorld(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        if (data.homeWorld == null)
            return null;

        Identifier worldId = Identifier.parse(data.homeWorld);
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
        return player.level().getServer().getLevel(worldKey);
    }

    public static void setHome(ServerPlayer player, Vec3 location, ServerLevel world) {
        PlayerData data = getPlayerData(player);
        data.homeLocation = location;
        data.homeWorld = world.dimension().identifier().toString();
        savePlayerData(player);
    }

    // Spawn methods
    public static Vec3 getSpawn() {
        return globalData.spawnLocation;
    }

    public static ServerLevel getSpawnWorld(MinecraftServer server) {
        if (globalData.spawnWorld == null) {
            return server.overworld();
        }

        Identifier worldId = Identifier.parse(globalData.spawnWorld);
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
        ServerLevel world = server.getLevel(worldKey);
        return world != null ? world : server.overworld();
    }

    public static void setSpawn(Vec3 location, ServerLevel world) {
        globalData.spawnLocation = location;
        globalData.spawnWorld = world.dimension().identifier().toString();
        saveGlobalData(world.getServer());
    }

    // Back/last location methods
    public static Vec3 getLastLocation(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.lastLocation;
    }

    public static ServerLevel getLastWorld(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        if (data.lastWorld == null)
            return null;

        Identifier worldId = Identifier.parse(data.lastWorld);
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
        return player.level().getServer().getLevel(worldKey);
    }

    public static void setLastLocation(ServerPlayer player, Vec3 location, ServerLevel world) {
        PlayerData data = getPlayerData(player);
        data.lastLocation = location;
        data.lastWorld = world.dimension().identifier().toString();
        savePlayerData(player);
    }

    // God mode methods
    public static boolean getGodMode(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.godMode;
    }

    public static void setGodMode(ServerPlayer player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.godMode = enabled;
        savePlayerData(player);
    }

    // Fly methods
    public static boolean getFlyEnabled(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.flyEnabled;
    }

    public static void setFlyEnabled(ServerPlayer player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.flyEnabled = enabled;
        savePlayerData(player);
    }

    // Mute methods
    public static boolean isMuted(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.muted;
    }

    public static void setMuted(ServerPlayer player, boolean muted) {
        PlayerData data = getPlayerData(player);
        data.muted = muted;
        savePlayerData(player);
    }

    // Temporary ban methods
    public static boolean isTempBanned(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        if (data.tempBanUntil <= 0)
            return false;

        // Check if ban has expired
        if (System.currentTimeMillis() >= data.tempBanUntil) {
            // Ban expired, clear it
            data.tempBanUntil = 0;
            data.tempBanReason = "";
            savePlayerData(player);
            return false;
        }

        return true;
    }

    public static void setTempBan(ServerPlayer player, long banUntil, String reason) {
        PlayerData data = getPlayerData(player);
        data.tempBanUntil = banUntil;
        data.tempBanReason = reason;
        savePlayerData(player);
    }

    public static String getTempBanReason(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.tempBanReason != null ? data.tempBanReason : "";
    }

    public static long getTempBanExpiry(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.tempBanUntil;
    }

    public static void clearTempBan(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        data.tempBanUntil = 0;
        data.tempBanReason = "";
        savePlayerData(player);
    }

    // Keep Inventory methods
    public static boolean getKeepInventory(ServerPlayer player) {
        return getPlayerData(player).keepInventory;
    }

    public static void setKeepInventory(ServerPlayer player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.keepInventory = enabled;
        savePlayerData(player);
    }

    // UUID-based methods for offline players
    public static Vec3 getLastLocationByUUID(java.util.UUID uuid) {
        try {
            Path playerFile = getPlayerDataPath(uuid);
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                PlayerData data = GSON.fromJson(json, PlayerData.class);
                return data != null ? data.lastLocation : null;
            }
        } catch (IOException e) {
            System.err.println("Failed to load player data for UUID " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    public static ServerLevel getLastWorldByUUID(java.util.UUID uuid, MinecraftServer server) {
        try {
            Path playerFile = getPlayerDataPath(uuid);
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                PlayerData data = GSON.fromJson(json, PlayerData.class);
                if (data != null && data.lastWorld != null) {
                    Identifier worldId = Identifier.parse(data.lastWorld);
                    ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, worldId);
                    return server.getLevel(worldKey);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load player data for UUID " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    private static Path getPlayerDataPath(java.util.UUID uuid) {
        if (serverInstance == null) {
            return null;
        }
        return serverInstance.getServerDirectory()
                .resolve(DATA_FOLDER)
                .resolve(PLAYERS_FOLDER)
                .resolve(uuid.toString() + ".json");
    }
    
    // Inventory Storage methods for Keep Inventory feature
    public static void storeInventoryOnDeath(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        data.storedInventory = new StoredInventoryData(player);
        // Don't save to disk immediately - inventory data is transient
        // It will be saved when the player data is next saved
    }
    
    public static boolean hasStoredInventory(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        return data.storedInventory != null;
    }
    
    public static void restoreInventoryOnRespawn(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        if (data.storedInventory != null) {
            data.storedInventory.restore(player);
            data.storedInventory = null; // Clear stored inventory after restoration
        }
    }
    
    public static void clearStoredInventory(ServerPlayer player) {
        PlayerData data = getPlayerData(player);
        data.storedInventory = null;
    }
}
