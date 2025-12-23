
package com.fleettools.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlayerDataManager {
    // --- Warp System ---
    public static class WarpData {
        public Vec3d location;
        public String world;

        public WarpData() {
        }

        public WarpData(Vec3d location, String world) {
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
            Path warpsFile = server.getRunDirectory().resolve(DATA_FOLDER).resolve(WARPS_FILE);
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
            Path warpsFile = server.getRunDirectory().resolve(DATA_FOLDER).resolve(WARPS_FILE);
            String json = GSON.toJson(warps, WARP_MAP_TYPE);
            Files.writeString(warpsFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save warps: " + e.getMessage());
        }
    }

    public static Map<String, WarpData> getWarps() {
        return warps;
    }

    public static void setWarp(String name, Vec3d location, ServerWorld world) {
        warps.put(name, new WarpData(location, world.getRegistryKey().getValue().toString()));
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
    public static boolean removeHome(ServerPlayerEntity player) {
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
        public Vec3d homeLocation;
        public String homeWorld;
        public Vec3d lastLocation;
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
        public ItemStack[] mainInventory;
        public ItemStack[] armorInventory;
        public ItemStack[] offhandInventory;
        public int selectedSlot;
        public long deathTime;
        
        public StoredInventoryData() {
        }
        
        public StoredInventoryData(ServerPlayerEntity player) {
            this.mainInventory = new ItemStack[36];
            this.armorInventory = new ItemStack[4];
            this.offhandInventory = new ItemStack[1];
            com.fleettools.mixin.accessor.PlayerInventoryAccessor inv = (com.fleettools.mixin.accessor.PlayerInventoryAccessor) player.getInventory();
            this.selectedSlot = inv.getSelectedSlot();
            this.deathTime = System.currentTimeMillis();
            
            // Copy inventory contents
            for (int i = 0; i < 36; i++) {
                this.mainInventory[i] = inv.getMain().get(i).copy();
            }
            // Copy armor - armor slots are 36-39 in combined list
            for (int i = 0; i < 4; i++) {
                this.armorInventory[i] = player.getInventory().getStack(36 + i).copy();
            }
            // Copy offhand - offhand is slot 40 in combined list
            this.offhandInventory[0] = player.getInventory().getStack(40).copy();
        }
        
        public void restore(ServerPlayerEntity player) {
            // Clear current inventory
            player.getInventory().clear();
            
            com.fleettools.mixin.accessor.PlayerInventoryAccessor inv = (com.fleettools.mixin.accessor.PlayerInventoryAccessor) player.getInventory();
            
            // Restore saved inventory
            for (int i = 0; i < 36; i++) {
                inv.getMain().set(i, this.mainInventory[i].copy());
            }
            // Restore armor - armor slots are 36-39 in combined list
            for (int i = 0; i < 4; i++) {
                player.getInventory().setStack(36 + i, this.armorInventory[i].copy());
            }
            // Restore offhand - offhand is slot 40 in combined list
            player.getInventory().setStack(40, this.offhandInventory[0].copy());
            inv.setSelectedSlot(this.selectedSlot);
            
            // Mark inventory as changed
            player.currentScreenHandler.sendContentUpdates();
            player.playerScreenHandler.onContentChanged(player.getInventory());
            player.sendAbilitiesUpdate();
        }
    }

    public static class GlobalData {
        public Vec3d spawnLocation;
        public String spawnWorld;

        public GlobalData() {
        }
    }

    public static void init(MinecraftServer server) {
        serverInstance = server; // Store server instance for later use
        try {
            Path dataDir = server.getRunDirectory().resolve(DATA_FOLDER);
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
            Path globalFile = server.getRunDirectory().resolve(DATA_FOLDER).resolve(GLOBAL_DATA_FILE);
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
            Path globalFile = server.getRunDirectory().resolve(DATA_FOLDER).resolve(GLOBAL_DATA_FILE);
            String json = GSON.toJson(globalData);
            Files.writeString(globalFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save global data: " + e.getMessage());
        }
    }

    public static PlayerData getPlayerData(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        // Load from file
        try {
            Path playerFile = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getRunDirectory()
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
                    .println("Failed to load player data for " + player.getGameProfile().name() + ": " + e.getMessage());
        }

        // Create new data
        PlayerData data = new PlayerData();
        playerDataCache.put(uuid, data);
        return data;
    }

    public static void savePlayerData(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerData data = playerDataCache.get(uuid);

        if (data == null)
            return;

        try {
            Path playerFile = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getRunDirectory()
                    .resolve(DATA_FOLDER)
                    .resolve(PLAYERS_FOLDER)
                    .resolve(uuid.toString() + ".json");

            String json = GSON.toJson(data);
            Files.writeString(playerFile, json);
        } catch (IOException e) {
            System.err
                    .println("Failed to save player data for " + player.getGameProfile().name() + ": " + e.getMessage());
        }
    }

    // Home methods
    public static Vec3d getHome(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.homeLocation;
    }

    public static ServerWorld getHomeWorld(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        if (data.homeWorld == null)
            return null;

        Identifier worldId = Identifier.of(data.homeWorld);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        return ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getWorld(worldKey);
    }

    public static void setHome(ServerPlayerEntity player, Vec3d location, ServerWorld world) {
        PlayerData data = getPlayerData(player);
        data.homeLocation = location;
        data.homeWorld = world.getRegistryKey().getValue().toString();
        savePlayerData(player);
    }

    // Spawn methods
    public static Vec3d getSpawn() {
        return globalData.spawnLocation;
    }

    public static ServerWorld getSpawnWorld(MinecraftServer server) {
        if (globalData.spawnWorld == null) {
            return server.getOverworld();
        }

        Identifier worldId = Identifier.of(globalData.spawnWorld);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        ServerWorld world = server.getWorld(worldKey);
        return world != null ? world : server.getOverworld();
    }

    public static void setSpawn(Vec3d location, ServerWorld world) {
        globalData.spawnLocation = location;
        globalData.spawnWorld = world.getRegistryKey().getValue().toString();
        saveGlobalData(world.getServer());
    }

    // Back/last location methods
    public static Vec3d getLastLocation(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.lastLocation;
    }

    public static ServerWorld getLastWorld(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        if (data.lastWorld == null)
            return null;

        Identifier worldId = Identifier.of(data.lastWorld);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        return ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getWorld(worldKey);
    }

    public static void setLastLocation(ServerPlayerEntity player, Vec3d location, ServerWorld world) {
        PlayerData data = getPlayerData(player);
        data.lastLocation = location;
        data.lastWorld = world.getRegistryKey().getValue().toString();
        savePlayerData(player);
    }

    // God mode methods
    public static boolean getGodMode(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.godMode;
    }

    public static void setGodMode(ServerPlayerEntity player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.godMode = enabled;
        savePlayerData(player);
    }

    // Fly methods
    public static boolean getFlyEnabled(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.flyEnabled;
    }

    public static void setFlyEnabled(ServerPlayerEntity player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.flyEnabled = enabled;
        savePlayerData(player);
    }

    // Mute methods
    public static boolean isMuted(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.muted;
    }

    public static void setMuted(ServerPlayerEntity player, boolean muted) {
        PlayerData data = getPlayerData(player);
        data.muted = muted;
        savePlayerData(player);
    }

    // Temporary ban methods
    public static boolean isTempBanned(ServerPlayerEntity player) {
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

    public static void setTempBan(ServerPlayerEntity player, long banUntil, String reason) {
        PlayerData data = getPlayerData(player);
        data.tempBanUntil = banUntil;
        data.tempBanReason = reason;
        savePlayerData(player);
    }

    public static String getTempBanReason(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.tempBanReason != null ? data.tempBanReason : "";
    }

    public static long getTempBanExpiry(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.tempBanUntil;
    }

    public static void clearTempBan(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        data.tempBanUntil = 0;
        data.tempBanReason = "";
        savePlayerData(player);
    }

    // Keep Inventory methods
    public static boolean getKeepInventory(ServerPlayerEntity player) {
        return getPlayerData(player).keepInventory;
    }

    public static void setKeepInventory(ServerPlayerEntity player, boolean enabled) {
        PlayerData data = getPlayerData(player);
        data.keepInventory = enabled;
        savePlayerData(player);
    }

    // UUID-based methods for offline players
    public static Vec3d getLastLocationByUUID(java.util.UUID uuid) {
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

    public static ServerWorld getLastWorldByUUID(java.util.UUID uuid, MinecraftServer server) {
        try {
            Path playerFile = getPlayerDataPath(uuid);
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                PlayerData data = GSON.fromJson(json, PlayerData.class);
                if (data != null && data.lastWorld != null) {
                    Identifier worldId = Identifier.of(data.lastWorld);
                    RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
                    return server.getWorld(worldKey);
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
        return serverInstance.getRunDirectory()
                .resolve(DATA_FOLDER)
                .resolve(PLAYERS_FOLDER)
                .resolve(uuid.toString() + ".json");
    }
    
    // Inventory Storage methods for Keep Inventory feature
    public static void storeInventoryOnDeath(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        data.storedInventory = new StoredInventoryData(player);
        // Don't save to disk immediately - inventory data is transient
        // It will be saved when the player data is next saved
    }
    
    public static boolean hasStoredInventory(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.storedInventory != null;
    }
    
    public static void restoreInventoryOnRespawn(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        if (data.storedInventory != null) {
            data.storedInventory.restore(player);
            data.storedInventory = null; // Clear stored inventory after restoration
        }
    }
    
    public static void clearStoredInventory(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        data.storedInventory = null;
    }
}
