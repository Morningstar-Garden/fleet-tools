
package com.fleettools.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
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

        public NbtCompound writeNbt(NbtCompound nbt) {
            nbt.putDouble("x", location.x);
            nbt.putDouble("y", location.y);
            nbt.putDouble("z", location.z);
            nbt.putString("world", world);
            return nbt;
        }

        public static WarpData fromNbt(NbtCompound nbt) {
            double x = nbt.contains("x") ? nbt.getDouble("x").orElse(0.0) : 0.0;
            double y = nbt.contains("y") ? nbt.getDouble("y").orElse(0.0) : 0.0;
            double z = nbt.contains("z") ? nbt.getDouble("z").orElse(0.0) : 0.0;
            Vec3d location = new Vec3d(x, y, z);
            String world = nbt.contains("world") ? nbt.getString("world").orElse("") : "";
            return new WarpData(location, world);
        }
    }

    private static final String WARPS_FILE = "warps.nbt";
    private static Map<String, WarpData> warps = new HashMap<>();

    public static void loadWarps(MinecraftServer server) {
        try {
            Path dataDir = server.getRunDirectory().resolve(DATA_FOLDER);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            Path warpsFile = dataDir.resolve(WARPS_FILE);
            if (Files.exists(warpsFile)) {
                NbtCompound nbt = NbtIo.read(warpsFile);
                if (nbt != null) {
                    warps.clear();
                    for (String key : nbt.getKeys()) {
                        if (nbt.contains(key)) {
                            NbtCompound warpNbt = nbt.getCompound(key).orElse(new NbtCompound());
                            warps.put(key, WarpData.fromNbt(warpNbt));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load warps: " + e.getMessage());
        }
    }

    public static void saveWarps(MinecraftServer server) {
        try {
            Path dataDir = server.getRunDirectory().resolve(DATA_FOLDER);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            Path warpsFile = dataDir.resolve(WARPS_FILE);
            NbtCompound nbt = new NbtCompound();
            
            for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
                NbtCompound warpNbt = new NbtCompound();
                entry.getValue().writeNbt(warpNbt);
                nbt.put(entry.getKey(), warpNbt);
            }
            
            NbtIo.write(nbt, warpsFile);
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

    private static final String DATA_FOLDER = "fleettools";
    private static final String PLAYERS_FOLDER = "players";
    private static final String GLOBAL_DATA_FILE = "global.nbt";

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
        public boolean keepInventoryDisabled = false; // If true, player won't keep inventory on death (opt-out)
        public long tempBanUntil = 0; // Timestamp when temp ban expires (0 = not banned)
        public String tempBanReason = "";

        public PlayerData() {
        }

        public NbtCompound writeNbt(NbtCompound nbt) {
            if (homeLocation != null) {
                NbtCompound homePos = new NbtCompound();
                homePos.putDouble("x", homeLocation.x);
                homePos.putDouble("y", homeLocation.y);
                homePos.putDouble("z", homeLocation.z);
                nbt.put("homeLocation", homePos);
            }
            if (homeWorld != null) nbt.putString("homeWorld", homeWorld);
            
            if (lastLocation != null) {
                NbtCompound lastPos = new NbtCompound();
                lastPos.putDouble("x", lastLocation.x);
                lastPos.putDouble("y", lastLocation.y);
                lastPos.putDouble("z", lastLocation.z);
                nbt.put("lastLocation", lastPos);
            }
            if (lastWorld != null) nbt.putString("lastWorld", lastWorld);
            
            nbt.putBoolean("godMode", godMode);
            nbt.putBoolean("flyEnabled", flyEnabled);
            nbt.putBoolean("muted", muted);
            nbt.putBoolean("keepInventoryDisabled", keepInventoryDisabled);
            nbt.putLong("tempBanUntil", tempBanUntil);
            nbt.putString("tempBanReason", tempBanReason != null ? tempBanReason : "");
            
            return nbt;
        }

        public static PlayerData fromNbt(NbtCompound nbt) {
            PlayerData data = new PlayerData();
            
            if (nbt.contains("homeLocation")) {
                NbtCompound homePos = nbt.getCompound("homeLocation").orElse(new NbtCompound());
                double x = homePos.getDouble("x").orElse(0.0);
                double y = homePos.getDouble("y").orElse(0.0); 
                double z = homePos.getDouble("z").orElse(0.0);
                data.homeLocation = new Vec3d(x, y, z);
            }
            data.homeWorld = nbt.contains("homeWorld") ? nbt.getString("homeWorld").orElse(null) : null;
            
            if (nbt.contains("lastLocation")) {
                NbtCompound lastPos = nbt.getCompound("lastLocation").orElse(new NbtCompound());
                double x = lastPos.getDouble("x").orElse(0.0);
                double y = lastPos.getDouble("y").orElse(0.0);
                double z = lastPos.getDouble("z").orElse(0.0);
                data.lastLocation = new Vec3d(x, y, z);
            }
            data.lastWorld = nbt.contains("lastWorld") ? nbt.getString("lastWorld").orElse(null) : null;
            
            data.godMode = nbt.contains("godMode") && nbt.getBoolean("godMode").orElse(false);
            data.flyEnabled = nbt.contains("flyEnabled") && nbt.getBoolean("flyEnabled").orElse(false);
            data.muted = nbt.contains("muted") && nbt.getBoolean("muted").orElse(false);
            data.keepInventoryDisabled = nbt.contains("keepInventoryDisabled") && nbt.getBoolean("keepInventoryDisabled").orElse(false);
            data.tempBanUntil = nbt.contains("tempBanUntil") ? nbt.getLong("tempBanUntil").orElse(0L) : 0L;
            data.tempBanReason = nbt.contains("tempBanReason") ? nbt.getString("tempBanReason").orElse("") : "";
            
            return data;
        }
    }

    // Global data structure for server-wide settings
    public static class GlobalData {
        public Vec3d spawnLocation;
        public String spawnWorld;

        public GlobalData() {
        }

        public NbtCompound writeNbt(NbtCompound nbt) {
            if (spawnLocation != null) {
                NbtCompound spawnPos = new NbtCompound();
                spawnPos.putDouble("x", spawnLocation.x);
                spawnPos.putDouble("y", spawnLocation.y);
                spawnPos.putDouble("z", spawnLocation.z);
                nbt.put("spawnLocation", spawnPos);
            }
            if (spawnWorld != null) {
                nbt.putString("spawnWorld", spawnWorld);
            }
            return nbt;
        }

        public static GlobalData fromNbt(NbtCompound nbt) {
            GlobalData data = new GlobalData();
            if (nbt.contains("spawnLocation")) {
                NbtCompound spawnPos = nbt.getCompound("spawnLocation").orElse(new NbtCompound());
                double x = spawnPos.getDouble("x").orElse(0.0);
                double y = spawnPos.getDouble("y").orElse(0.0);
                double z = spawnPos.getDouble("z").orElse(0.0);
                data.spawnLocation = new Vec3d(x, y, z);
            }
            data.spawnWorld = nbt.contains("spawnWorld") ? nbt.getString("spawnWorld").orElse(null) : null;
            return data;
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
            Path dataDir = server.getRunDirectory().resolve(DATA_FOLDER);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            Path globalFile = dataDir.resolve(GLOBAL_DATA_FILE);
            if (Files.exists(globalFile)) {
                NbtCompound nbt = NbtIo.read(globalFile);
                if (nbt != null) {
                    globalData = GlobalData.fromNbt(nbt);
                } else {
                    globalData = new GlobalData();
                }
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
            Path dataDir = server.getRunDirectory().resolve(DATA_FOLDER);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            
            Path globalFile = dataDir.resolve(GLOBAL_DATA_FILE);
            NbtCompound nbt = new NbtCompound();
            globalData.writeNbt(nbt);
            NbtIo.write(nbt, globalFile);
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
                    .resolve(uuid.toString() + ".nbt");

            if (Files.exists(playerFile)) {
                NbtCompound nbt = NbtIo.read(playerFile);
                if (nbt != null) {
                    PlayerData data = PlayerData.fromNbt(nbt);
                    playerDataCache.put(uuid, data);
                    return data;
                }
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
            Path dataDir = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getRunDirectory()
                    .resolve(DATA_FOLDER);
            Path playersDir = dataDir.resolve(PLAYERS_FOLDER);
            
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            if (!Files.exists(playersDir)) {
                Files.createDirectories(playersDir);
            }
            
            Path playerFile = playersDir.resolve(uuid.toString() + ".nbt");

            // Serialize with NBT - much more reliable than JSON
            try {
                NbtCompound nbt = new NbtCompound();
                data.writeNbt(nbt);
                NbtIo.write(nbt, playerFile);
            } catch (Exception e) {
                System.err.println("[FleetTools] Failed to serialize player data: " + e.getMessage());
                throw new RuntimeException("Failed to save player data", e);
            }
            
        } catch (IOException e) {
            System.err
                    .println("[FleetTools] Failed to save player data for " + player.getGameProfile().name() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[FleetTools] Critical error in savePlayerData: " + e.getMessage());
            e.printStackTrace();
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
        try {
            if (player == null || location == null || world == null) {
                return; // Safety check for null parameters
            }
            
            PlayerData data = getPlayerData(player);
            if (data == null) {
                return; // Safety check for null data
            }
            
            data.lastLocation = location;
            data.lastWorld = world.getRegistryKey().getValue().toString();
            savePlayerData(player);
        } catch (Exception e) {
            // Silently handle errors during disconnect to avoid server crashes
            System.err.println("[FleetTools] Error saving last location for " + (player != null ? player.getGameProfile().name() : "unknown") + ": " + e.getMessage());
        }
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

    // Keep inventory methods
    public static boolean isKeepInventoryDisabled(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.keepInventoryDisabled;
    }

    public static void setKeepInventoryDisabled(ServerPlayerEntity player, boolean disabled) {
        PlayerData data = getPlayerData(player);
        data.keepInventoryDisabled = disabled;
        savePlayerData(player);
    }



    // UUID-based methods for offline players
    public static Vec3d getLastLocationByUUID(java.util.UUID uuid) {
        try {
            Path playerFile = getPlayerDataPath(uuid);
            if (Files.exists(playerFile)) {
                NbtCompound nbt = NbtIo.read(playerFile);
                if (nbt != null) {
                    PlayerData data = PlayerData.fromNbt(nbt);
                    return data != null ? data.lastLocation : null;
                }
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
                NbtCompound nbt = NbtIo.read(playerFile);
                if (nbt != null) {
                    PlayerData data = PlayerData.fromNbt(nbt);
                    if (data != null && data.lastWorld != null) {
                        Identifier worldId = Identifier.of(data.lastWorld);
                        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
                        return server.getWorld(worldKey);
                    }
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
                .resolve(uuid.toString() + ".nbt");
    }
    

}

