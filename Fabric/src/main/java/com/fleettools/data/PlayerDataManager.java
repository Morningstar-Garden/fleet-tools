
package com.fleettools.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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

    // Enhanced exclusion strategy to prevent serialization issues and circular references
    private static class SafeExclusionStrategy implements ExclusionStrategy {
        private static final Set<String> BLOCKED_CLASSES = Set.of(
            "java.util.Optional",
            "java.util.concurrent",
            "java.lang.ref",
            "java.lang.reflect",
            "java.lang.Thread",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Process", 
            "java.security",
            "sun.",
            "jdk.",
            "com.mojang.authlib",
            "net.minecraft.server",
            "net.minecraft.client",
            "net.minecraft.world.level",
            "net.minecraft.network",
            "net.fabricmc"
        );
        
        private static final Set<String> BLOCKED_FIELDS = Set.of(
            "server", "world", "level", "dimension", "chunk", "entity", "player",
            "networkHandler", "connection", "thread", "executor", "future",
            "callback", "listener", "handler", "manager", "registry",
            "accessor", "mixin", "fabric", "$", "class", "classloader",
            "method", "field", "constructor", "annotation", "eetop",
            "threadlocals", "inheritedthreadlocals", "contextclassloader"
        );
        
        // Thread-local tracking to prevent infinite recursion
        private static final ThreadLocal<Set<Object>> SERIALIZATION_STACK = ThreadLocal.withInitial(HashSet::new);
        private static final int MAX_DEPTH = 10;
        private static final int MAX_ARRAY_SIZE = 1000;
        
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            try {
                // Skip Optional fields and other problematic types
                if (field.getDeclaredType().equals(Optional.class)) {
                    return true;
                }
                
                String fieldName = field.getName().toLowerCase();
                String className = field.getDeclaringClass().getName();
                
                // Check blocked field names
                for (String blockedField : BLOCKED_FIELDS) {
                    if (fieldName.contains(blockedField)) {
                        return true;
                    }
                }
                
                // Check blocked class prefixes
                for (String blockedClass : BLOCKED_CLASSES) {
                    if (className.startsWith(blockedClass)) {
                        return true;
                    }
                }
                
                // Skip synthetic fields
                if (fieldName.contains("$") || fieldName.startsWith("_")) {
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                // If we can't determine if we should skip, skip it to be safe
                return true;
            }
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            try {
                String className = clazz.getName();
                
                // Skip Optional and other problematic classes specifically
                if (clazz.equals(Optional.class) || 
                    clazz.equals(Thread.class) ||
                    clazz.equals(Class.class) ||
                    clazz.equals(ClassLoader.class)) {
                    return true;
                }
                
                // Skip all java.lang classes except basic types
                if (className.startsWith("java.lang.") && 
                    !className.equals("java.lang.String") &&
                    !className.equals("java.lang.Integer") &&
                    !className.equals("java.lang.Long") &&
                    !className.equals("java.lang.Double") &&
                    !className.equals("java.lang.Float") &&
                    !className.equals("java.lang.Boolean") &&
                    !className.equals("java.lang.Byte") &&
                    !className.equals("java.lang.Short") &&
                    !className.equals("java.lang.Character")) {
                    return true;
                }
                
                // Check blocked class prefixes
                for (String blockedClass : BLOCKED_CLASSES) {
                    if (className.startsWith(blockedClass)) {
                        return true;
                    }
                }
                
                // Skip lambda classes and synthetic classes
                if (className.contains("$Lambda") || className.contains("$$")) {
                    return true;
                }
                
                return false;
            } catch (Exception e) {
                // If we can't determine if we should skip, skip it to be safe
                return true;
            }
        }
    }
    
    private static final Gson GSON = new GsonBuilder()
            .setExclusionStrategies(new SafeExclusionStrategy())
            .setLenient()
            .create();
    private static final String DATA_FOLDER = "fleettools";
    private static final String PLAYERS_FOLDER = "players";
    private static final String GLOBAL_DATA_FILE = "global.json";

    private static final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private static GlobalData globalData;
    private static MinecraftServer serverInstance;

    // Mod compatibility flags
    private static Boolean battleCorePresent = null;
    private static Boolean safeslotPresent = null;
    
    /**
     * Check if BattleCore mod is present to avoid inventory conflicts during battles
     */
    private static boolean isBattleCorePresent() {
        if (battleCorePresent == null) {
            try {
                Class.forName("com.battlecore.battle.Battle");
                battleCorePresent = true;
            } catch (ClassNotFoundException e) {
                battleCorePresent = false;
            }
        }
        return battleCorePresent;
    }
    
    /**
     * Check if SafeSlot mod is present
     */
    private static boolean isSafeslotPresent() {
        if (safeslotPresent == null) {
            try {
                Class.forName("com.safeslot.SafeslotMod");
                safeslotPresent = true;
            } catch (ClassNotFoundException e) {
                safeslotPresent = false;
            }
        }
        return safeslotPresent;
    }
    
    /**
     * Check if player is in an active BattleCore battle
     */
    private static boolean isPlayerInBattle(ServerPlayerEntity player) {
        if (!isBattleCorePresent()) {
            return false;
        }
        try {
            Class<?> battleClass = Class.forName("com.battlecore.battle.Battle");
            Object battle = battleClass.getMethod("getInstance").invoke(null);
            Object state = battleClass.getMethod("getState").invoke(battle);
            
            // Check if battle is active (not INACTIVE)
            if (!state.toString().equals("INACTIVE")) {
                Boolean inBattle = (Boolean) battleClass.getMethod("isPlayerInBattle", java.util.UUID.class)
                    .invoke(battle, player.getUuid());
                return inBattle != null && inBattle;
            }
        } catch (Exception e) {
            // If we can't check, assume not in battle
        }
        return false;
    }

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
        public StoredInventoryData storedInventory = null;
        // Safety tracking for keep inventory
        public boolean hasPendingInventoryRestore = false;
        public long lastDeathTime = 0;
        public int inventoryRestoreAttempts = 0;
        public boolean isOnDeathScreen = false;

        public PlayerData() {
        }
    }
    
    public static class StoredInventoryData {
        public ItemStack[] allInventorySlots;  // Store ALL inventory slots dynamically
        public int inventorySize;              // Track the total size for validation
        public int selectedSlot;
        public long deathTime;
        public ItemStack nemosBackpack = null; // Store Nemo's Backpack separately
        // Safety tracking
        public boolean isValidRestore = true; // Flag to track if this data is safe to restore
        public String deathCause = "unknown"; // Track how the death occurred
        public long creationTime; // When this inventory was stored
        
        public StoredInventoryData() {
            this.creationTime = System.currentTimeMillis();
        }
        
        public StoredInventoryData(ServerPlayerEntity player) {
            com.fleettools.mixin.accessor.PlayerInventoryAccessor inv = (com.fleettools.mixin.accessor.PlayerInventoryAccessor) player.getInventory();
            this.selectedSlot = inv.getSelectedSlot();
            this.deathTime = System.currentTimeMillis();
            this.creationTime = System.currentTimeMillis(); // FIX: Set creation time properly
            
            // Get the total size of the player's inventory (including modded slots)
            this.inventorySize = player.getInventory().size();
            this.allInventorySlots = new ItemStack[this.inventorySize];
            
            // Copy ALL inventory slots dynamically - this will include any modded slots
            for (int i = 0; i < this.inventorySize; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                this.allInventorySlots[i] = stack != null ? stack.copy() : ItemStack.EMPTY.copy();
            }
            
            // NEMO'S BACKPACKS: Check if the player inventory implements BackpackGetter
            try {
                Class<?> backpackGetterClass = Class.forName("com.nemonotfound.nemos.backpacks.helper.BackpackGetter");
                if (backpackGetterClass.isInstance(player.getInventory())) {
                    // Use reflection to call nemosBackpacks$getBackpack()
                    Object backpackGetter = player.getInventory();
                    java.lang.reflect.Method getBackpackMethod = backpackGetterClass.getMethod("nemosBackpacks$getBackpack");
                    ItemStack backpack = (ItemStack) getBackpackMethod.invoke(backpackGetter);
                    
                    if (backpack != null && !backpack.isEmpty()) {
                        this.nemosBackpack = backpack.copy();
                    }
                }
            } catch (Exception e) {
                // Nemo's Backpacks not installed or error accessing backpack
            }
        }
        
        public boolean restore(ServerPlayerEntity player) {
            // Don't restore if player is in BattleCore battle - let BattleCore handle it
            if (isPlayerInBattle(player)) {
                return false;
            }
            
            try {
                // Validation checks
                if (this.allInventorySlots == null) {
                    System.err.println("[FleetTools] Cannot restore inventory: stored slots are null");
                    this.isValidRestore = false;
                    return false;
                }
                
                if (!this.isValidRestore) {
                    System.err.println("[FleetTools] Cannot restore inventory: data marked as invalid");
                    return false;
                }
                
                // Clear current inventory first (with safety check)
                try {
                    player.getInventory().clear();
                } catch (Exception e) {
                    System.err.println("[FleetTools] Warning: Failed to clear inventory before restore: " + e.getMessage());
                }
                
                com.fleettools.mixin.accessor.PlayerInventoryAccessor inv = (com.fleettools.mixin.accessor.PlayerInventoryAccessor) player.getInventory();
                
                // Validate that inventory size matches (important for modded inventories)
                int currentSize = player.getInventory().size();
                if (currentSize != this.inventorySize) {
                    System.out.println("[FleetTools] Inventory size mismatch: stored=" + this.inventorySize + ", current=" + currentSize);
                    // Use the smaller size to avoid index out of bounds
                    currentSize = Math.min(currentSize, this.inventorySize);
                }
                
                int restoredSlots = 0;
                int failedSlots = 0;
                
                // Restore ALL inventory slots dynamically - this includes modded slots
                for (int i = 0; i < currentSize && i < this.allInventorySlots.length; i++) {
                    if (this.allInventorySlots[i] != null) {
                        try {
                            ItemStack stackToRestore = this.allInventorySlots[i].copy();
                            player.getInventory().setStack(i, stackToRestore);
                            restoredSlots++;
                        } catch (Exception e) {
                            failedSlots++;
                            System.err.println("[FleetTools] Failed to restore slot " + i + ": " + e.getMessage());
                        }
                    }
                }
                
                // Restore selected slot (with bounds checking)
                try {
                    if (this.selectedSlot >= 0 && this.selectedSlot < 9) {
                        inv.setSelectedSlot(this.selectedSlot);
                    }
                } catch (Exception e) {
                    System.err.println("[FleetTools] Failed to restore selected slot: " + e.getMessage());
                }
                
                // NEMO'S BACKPACKS: Restore the backpack if we saved one
                if (this.nemosBackpack != null && !this.nemosBackpack.isEmpty()) {
                    try {
                        // Use setStack to place the backpack in slot 46 - this should trigger the mixin
                        player.getInventory().setStack(46, this.nemosBackpack.copy());
                        System.out.println("[FleetTools] Restored Nemo's backpack for " + player.getGameProfile().name());
                    } catch (Exception e) {
                        System.err.println("[FleetTools] Failed to restore Nemo's backpack: " + e.getMessage());
                    }
                }
                
                // Mark inventory as changed
                try {
                    player.currentScreenHandler.sendContentUpdates();
                    player.playerScreenHandler.onContentChanged(player.getInventory());
                    player.sendAbilitiesUpdate();
                } catch (Exception e) {
                    System.err.println("[FleetTools] Failed to update inventory display: " + e.getMessage());
                }
                
                System.out.println("[FleetTools] Inventory restoration complete: " + restoredSlots + " slots restored, " + failedSlots + " failed");
                
                // Consider restoration successful if we restored most slots
                return failedSlots < (restoredSlots / 2);
                
            } catch (Exception e) {
                System.err.println("[FleetTools] Critical error during inventory restoration: " + e.getMessage());
                e.printStackTrace();
                this.isValidRestore = false;
                return false;
            }
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
            // Pre-validation to prevent serialization issues
            if (data.storedInventory != null && data.storedInventory.allInventorySlots != null) {
                // Validate inventory data size
                if (data.storedInventory.allInventorySlots.length > 200) {
                    System.err.println("[FleetTools] Warning: Large inventory detected (" + data.storedInventory.allInventorySlots.length + " slots), truncating");
                    // Truncate to reasonable size
                    ItemStack[] truncated = new ItemStack[Math.min(data.storedInventory.allInventorySlots.length, 200)];
                    System.arraycopy(data.storedInventory.allInventorySlots, 0, truncated, 0, truncated.length);
                    data.storedInventory.allInventorySlots = truncated;
                    data.storedInventory.inventorySize = truncated.length;
                }
                
                // Validate individual ItemStacks
                for (int i = 0; i < data.storedInventory.allInventorySlots.length; i++) {
                    ItemStack stack = data.storedInventory.allInventorySlots[i];
                    if (stack != null && stack.getCount() > 64) {
                        // Fix invalid stack sizes
                        data.storedInventory.allInventorySlots[i] = stack.copyWithCount(Math.min(stack.getCount(), 64));
                    }
                }
            }
            
            Path playerFile = ((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer().getRunDirectory()
                    .resolve(DATA_FOLDER)
                    .resolve(PLAYERS_FOLDER)
                    .resolve(uuid.toString() + ".json");

            // Serialize with try-catch to handle any issues
            String json;
            try {
                json = GSON.toJson(data);
                
                // Safety check: if JSON is too large, something went wrong
                if (json.length() > 500_000) { // 500KB limit
                    System.err.println("[FleetTools] Error: Generated JSON too large (" + json.length() + " chars), attempting emergency save");
                    
                    // Emergency save: temporarily clear problematic data
                    StoredInventoryData backup = data.storedInventory;
                    data.storedInventory = null;
                    
                    json = GSON.toJson(data);
                    
                    // Restore data in memory
                    data.storedInventory = backup;
                    
                    System.err.println("[FleetTools] Emergency save completed, inventory data temporarily excluded");
                }
                
            } catch (Exception e) {
                System.err.println("[FleetTools] Failed to serialize player data: " + e.getMessage());
                return;
            }
            
            Files.writeString(playerFile, json);
            
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
        storeInventoryOnDeath(player, "death");
    }
    
    public static void storeInventoryOnDeath(ServerPlayerEntity player, String cause) {
        // Don't interfere if player is in an active BattleCore battle
        if (isPlayerInBattle(player)) {
            return;
        }
        
        try {
            PlayerData data = getPlayerData(player);
            
            // Safety: Clear any previous stored inventory to prevent conflicts
            if (data.storedInventory != null) {
                System.out.println("[FleetTools] Warning: Overwriting existing stored inventory for " + player.getGameProfile().name());
            }
            
            // Store new inventory data
            StoredInventoryData newInventoryData = new StoredInventoryData(player);
            newInventoryData.deathCause = cause;
            data.storedInventory = newInventoryData;
            
            // Set safety flags
            data.hasPendingInventoryRestore = true;
            data.lastDeathTime = System.currentTimeMillis();
            data.inventoryRestoreAttempts = 0;
            data.isOnDeathScreen = true;
            
            // Save to disk immediately for persistence
            savePlayerData(player);
            
            System.out.println("[FleetTools] Stored inventory for " + player.getGameProfile().name() + " (cause: " + cause + ")");
        } catch (Exception e) {
            System.err.println("[FleetTools] Failed to store inventory for " + player.getGameProfile().name() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static boolean hasStoredInventory(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        return data.storedInventory != null;
    }
    
    public static void restoreInventoryOnRespawn(ServerPlayerEntity player) {
        restoreInventoryOnRespawn(player, false);
    }
    
    public static void restoreInventoryOnRespawn(ServerPlayerEntity player, boolean forceRestore) {
        // Don't interfere if player is in an active BattleCore battle
        if (isPlayerInBattle(player)) {
            return;
        }
        
        try {
            PlayerData data = getPlayerData(player);
            
            if (data.storedInventory == null && !data.hasPendingInventoryRestore) {
                return; // No inventory to restore
            }
            
            // Safety check: Don't restore if too many attempts have been made
            if (!forceRestore && data.inventoryRestoreAttempts >= 3) {
                System.err.println("[FleetTools] Too many restoration attempts for " + player.getGameProfile().name() + ", clearing stored data");
                clearStoredInventoryCompletely(player);
                return;
            }
            
            // Safety check: Don't restore very old inventory (older than 5 minutes)
            if (!forceRestore && data.storedInventory != null && 
                (System.currentTimeMillis() - data.storedInventory.creationTime) > 300000) {
                System.err.println("[FleetTools] Stored inventory too old for " + player.getGameProfile().name() + ", clearing");
                clearStoredInventoryCompletely(player);
                return;
            }
            
            data.inventoryRestoreAttempts++;
            
            if (data.storedInventory != null && data.storedInventory.isValidRestore) {
                // Attempt restoration
                boolean restoreSuccess = data.storedInventory.restore(player);
                
                if (restoreSuccess || forceRestore) {
                    // Clear stored inventory after successful restoration
                    data.storedInventory = null;
                    data.hasPendingInventoryRestore = false;
                    data.isOnDeathScreen = false;
                    data.inventoryRestoreAttempts = 0;
                    
                    System.out.println("[FleetTools] Successfully restored inventory for " + player.getGameProfile().name());
                } else {
                    System.err.println("[FleetTools] Failed to restore inventory for " + player.getGameProfile().name() + " (attempt " + data.inventoryRestoreAttempts + ")");
                }
            } else {
                // Mark pending restore as complete even if we can't restore
                data.hasPendingInventoryRestore = false;
                data.isOnDeathScreen = false;
            }
            
            savePlayerData(player); // Save the state
            
        } catch (Exception e) {
            System.err.println("[FleetTools] Exception during inventory restoration for " + player.getGameProfile().name() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Clear problematic data to prevent repeated failures
            try {
                clearStoredInventoryCompletely(player);
            } catch (Exception clearEx) {
                System.err.println("[FleetTools] Failed to clear problematic inventory data: " + clearEx.getMessage());
            }
        }
    }
    
    public static void clearStoredInventory(ServerPlayerEntity player) {
        PlayerData data = getPlayerData(player);
        data.storedInventory = null;
        savePlayerData(player);
    }
    
    // Comprehensive cleanup method for safety
    public static void clearStoredInventoryCompletely(ServerPlayerEntity player) {
        try {
            PlayerData data = getPlayerData(player);
            data.storedInventory = null;
            data.hasPendingInventoryRestore = false;
            data.isOnDeathScreen = false;
            data.inventoryRestoreAttempts = 0;
            savePlayerData(player);
            
            System.out.println("[FleetTools] Completely cleared stored inventory data for " + player.getGameProfile().name());
        } catch (Exception e) {
            System.err.println("[FleetTools] Failed to clear stored inventory data: " + e.getMessage());
        }
    }
    
    // Emergency inventory restoration method
    public static void emergencyRestoreInventory(ServerPlayerEntity player) {
        System.out.println("[FleetTools] Performing emergency inventory restoration for " + player.getGameProfile().name());
        restoreInventoryOnRespawn(player, true); // Force restore
    }
    
    // Check if player needs inventory restoration
    public static boolean needsInventoryRestoration(ServerPlayerEntity player) {
        try {
            if (player == null) return false;
            PlayerData data = getPlayerData(player);
            if (data == null) return false;
            return data.hasPendingInventoryRestore || data.storedInventory != null;
        } catch (Exception e) {
            // Silently handle errors during disconnect to avoid server crashes
            return false;
        }
    }
    
    // Mark player as no longer on death screen
    public static void markPlayerOffDeathScreen(ServerPlayerEntity player) {
        try {
            PlayerData data = getPlayerData(player);
            data.isOnDeathScreen = false;
            savePlayerData(player);
        } catch (Exception e) {
            System.err.println("[FleetTools] Failed to update death screen status: " + e.getMessage());
        }
    }
    
    // Safety check for inventory data integrity
    public static void validateStoredInventory(ServerPlayerEntity player) {
        try {
            PlayerData data = getPlayerData(player);
            if (data.storedInventory != null) {
                // Check if stored inventory is too old
                long age = System.currentTimeMillis() - data.storedInventory.creationTime;
                if (age > 600000) { // 10 minutes
                    System.out.println("[FleetTools] Stored inventory for " + player.getGameProfile().name() + " is very old (" + (age / 1000) + "s), marking for cleanup");
                    data.storedInventory.isValidRestore = false;
                }
                
                // Basic validation
                if (data.storedInventory.allInventorySlots == null || data.storedInventory.inventorySize <= 0) {
                    System.err.println("[FleetTools] Invalid stored inventory data for " + player.getGameProfile().name() + ", marking invalid");
                    data.storedInventory.isValidRestore = false;
                }
            }
        } catch (Exception e) {
            System.err.println("[FleetTools] Failed to validate stored inventory: " + e.getMessage());
        }
    }
}

