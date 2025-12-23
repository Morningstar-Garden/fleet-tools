#!/bin/bash

# Fix API changes for Minecraft 1.21.11
# Run from project root

FABRIC_SRC="Fabric/src/main/java/com/fleettools"

echo "Fixing API changes for 1.21.11..."

# Fix getServerWorld() calls - add import and use cast
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/player\.getServerWorld()/((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld())/g' {} \;

find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/target\.getServerWorld()/((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)target).getWorld())/g' {} \;

find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/sender\.getServerWorld()/((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)sender).getWorld())/g' {} \;

find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/onlineTarget\.getServerWorld()/((net.minecraft.server.world.ServerWorld)((com.fleettools.mixin.accessor.EntityAccessor)onlineTarget).getWorld())/g' {} \;

# Fix getWorld() calls
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/player\.getWorld()/((com.fleettools.mixin.accessor.EntityAccessor)player).getWorld()/g' {} \;

find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/target\.getWorld()/((com.fleettools.mixin.accessor.EntityAccessor)target).getWorld()/g' {} \;

# Fix getServer() calls
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/player\.getServer()/((com.fleettools.mixin.accessor.ServerPlayerEntityAccessor)player).getServer()/g' {} \;

# Fix teleport() signature - add Set.of() and false
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/\.teleport(\([^,]*\), \([^,]*\), \([^,]*\), \([^,]*\), \([^,]*\), \([^)]*\))/.teleport(\1, \2, \3, \4, java.util.Set.of(), \5, \6, false)/g' {} \;

# Fix new Identifier(single) to Identifier.of(single)
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/new Identifier(\([^,)]*\))/Identifier.of(\1)/g' {} \;

# Fix GameProfile.getName() to .name()
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/\.getName()/.name()/g' {} \;

# Fix GameProfile.getId() to .id()  
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/profile\.getId()/profile.id()/g' {} \;

# Fix targetProfile.getId() to .id()
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/targetProfile\.getId()/targetProfile.id()/g' {} \;

# Fix GameMode.getName() to .name()
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/gameMode\.getName()/gameMode.name()/g' {} \;

# Fix .toPath() (Path is already a Path, remove .toPath())
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/\.getRunDirectory()\.toPath()/.getRunDirectory()/g' {} \;

# Fix getUserCache() to getUserManager().getUserCache()
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/\.getUserCache()/.getUserManager().getUserCache()/g' {} \;

# Fix getSpawnPos() to getSpawnPos()
find "$FABRIC_SRC" -name "*.java" -type f -exec sed -i \
  's/spawnWorld\.getSpawnPos()/spawnWorld.getSpawnPos()/g' {} \;

echo "API fixes applied. Now fixing specific files..."
