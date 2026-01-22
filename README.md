# Fleet Tools - Essential Commands for Fabric

Fleet Tools is a comprehensive Fabric mod that brings essential server administration commands to Minecraft Fabric servers. This mod provides server administrators with teleportation, moderation, utility, and administrative commands with full permission support and tab completion.

## Features

### Home System

- **`/home`** - Teleport to your home location
- **`/sethome`** - Set your home at your current location
- **`/delhome`** - Delete your home location
- Permission: `fleettools.home`, `fleettools.sethome`, `fleettools.delhome` (default: operators only)

### Spawn System

- **`/spawn`** - Teleport to the server spawn
- **`/setspawn`** - Set the server spawn at your current location (admin only)
- Permission: `fleettools.spawn`, `fleettools.setspawn` (default: operators only)

### Back System

- **`/back`** - Return to your previous location
- Permission: `fleettools.back` (default: operators only)

### Teleportation

- **`/tpo <player>`** or **`/tpoffline <player>`** - Teleport to any player's location (online or offline)
- **`/top <player>`** - Teleport to the highest block above current position
- Permission: `fleettools.tpo`, `fleettools.top`, `fleettools.top.others` (default: operators only)

### Health & Hunger

- **`/heal [player]`** - Restore health to full and clear negative effects
- **`/feed [player]`** - Restore hunger and saturation to full
- Permission: `fleettools.heal`, `fleettools.heal.others`, `fleettools.feed`, `fleettools.feed.others` (default: operators only)

### Flight System

- **`/fly [player]`** - Toggle flight mode
- Permission: `fleettools.fly`, `fleettools.fly.others` (default: operators only)

### Game Mode

- **`/gamemode <mode> [player]`** - Change game mode
- **`/gmc [player]`** - Switch to Creative mode
- **`/gms [player]`** - Switch to Survival mode
- **`/gma [player]`** - Switch to Adventure mode
- **`/gmsp [player]`** - Switch to Spectator mode
- Permission: `fleettools.gamemode`, `fleettools.gamemode.others` (default: operators only)

### Keep Inventory System

- **`/keepinv`** - Toggle your personal keep inventory setting (enabled by default)
- **`/keepinv status`** - Check your current keep inventory status
- **`/keepinv <player>`** - Toggle keep inventory for another player (admin only)
- Permission: `fleettools.keepinv`, `fleettools.keepinv.others` (default: all players for self, operators for others)

The keep inventory system is an opt-out feature that allows players to keep their items on death without affecting the server's gamerule. By default, all players have keep inventory enabled, but they can turn it off if they prefer the vanilla experience. Players still lose XP when they die, maintaining some consequence for death.

This system works with modded inventories, trinkets, and backpacks, ensuring comprehensive item protection regardless of which inventory expansion mods are installed. **Full compatibility with Nemo's Backpacks and other inventory expansion mods** - the system dynamically detects and preserves ALL inventory slots, not just vanilla ones. **AFK players are fully supported** - inventory data is stored persistently and will be restored even if players don't respawn for extended periods or disconnect while dead.

### God Mode

- **`/god [player]`** - Toggle invulnerability
- Permission: `fleettools.god`, `fleettools.god.others` (default: operators only)

### Warps System

- **`/warp <name>`** - Teleport to a named warp location
- **`/setwarp <name>`** - Set a warp at your current location
- **`/delwarp <name>`** - Delete a named warp
- Permission: `fleettools.warp`, `fleettools.setwarp`, `fleettools.delwarp` (default: operators only)

### Moderation Tools

- **`/unban <player>`** - Remove player from ban list (substitute for /pardon)
- **`/mute <player>`** - Prevent player from sending chat messages
- **`/unmute <player>`** - Allow muted player to send chat messages again
- **`/tempban <player> <time> [reason]`** - Temporarily ban player with automatic expiry
  - Time formats: `30s`, `5m`, `2h`, `1d`, `7d`, etc.
- Permission: `fleettools.unban`, `fleettools.mute`, `fleettools.unmute`, `fleettools.tempban` (default: level 3)

### Communication

- **`/msg <player> <message>`** - Send private message to player with actionbar display
- Permission: `fleettools.msg` (default: operators only)

### Utility Commands

- **`/coords <player>`** - Display player's coordinates and world information
- **`/daylight-pause`** - Pause or resume the daylight cycle
- Permission: `fleettools.coords`, `fleettools.daylight` (default: operators only)

### Time & Weather Control

- **`/day`** - Set time to day (7:00 AM)
- **`/night`** - Set time to night (7:00 PM)
- **`/sun`** - Set weather to clear/sunny
- **`/rain`** - Set weather to rain
- **`/thunderstorm`** - Set weather to thunderstorm
- Permission: `fleettools.time`, `fleettools.weather` (default: operators only)

## Installation

1. Make sure you have Fabric Loader installed
2. Download the latest release from the releases page
3. Place the mod file in your `mods` folder
4. Install the required dependencies:
   - Fabric API
   - Fabric Permissions API
5. Restart your server

## Dependencies

- **Fabric API** - Core Fabric API
- **Fabric Permissions API** - Permission system integration

## Permissions

Fleet Tools uses the Fabric Permissions API for permission management. All commands have associated permissions that can be managed through compatible permission plugins like LuckPerms.

### Permission Nodes

| Command                     | Permission Node              | Default Level |
| --------------------------- | ---------------------------- | ------------- |
| `/home`                     | `fleettools.home`            | 2 (operators) |
| `/sethome`                  | `fleettools.sethome`         | 2 (operators) |
| `/delhome`                  | `fleettools.delhome`         | 2 (operators) |
| `/spawn`                    | `fleettools.spawn`           | 2 (operators) |
| `/setspawn`                 | `fleettools.setspawn`        | 2 (operators) |
| `/back`                     | `fleettools.back`            | 2 (operators) |
| `/tpo <player>`             | `fleettools.tpo`             | 2 (operators) |
| `/top`                      | `fleettools.top`             | 2 (operators) |
| `/top <player>`             | `fleettools.top.others`      | 2 (operators) |
| `/heal`                     | `fleettools.heal`            | 2 (operators) |
| `/heal <player>`            | `fleettools.heal.others`     | 2 (operators) |
| `/feed`                     | `fleettools.feed`            | 2 (operators) |
| `/feed <player>`            | `fleettools.feed.others`     | 2 (operators) |
| `/fly`                      | `fleettools.fly`             | 2 (operators) |
| `/fly <player>`             | `fleettools.fly.others`      | 2 (operators) |
| `/gamemode`                 | `fleettools.gamemode`        | 2 (operators) |
| `/gamemode <mode> <player>` | `fleettools.gamemode.others` | 2 (operators) |
| `/god`                      | `fleettools.god`             | 2 (operators) |
| `/god <player>`             | `fleettools.god.others`      | 2 (operators) |
| `/warp`                     | `fleettools.warp`            | 2 (operators) |
| `/setwarp`                  | `fleettools.setwarp`         | 2 (operators) |
| `/delwarp`                  | `fleettools.delwarp`         | 2 (operators) |
| `/unban`                    | `fleettools.unban`           | 3 (admins)    |
| `/mute`                     | `fleettools.mute`            | 3 (admins)    |
| `/unmute`                   | `fleettools.unmute`          | 3 (admins)    |
| `/tempban`                  | `fleettools.tempban`         | 3 (admins)    |
| `/msg`                      | `fleettools.msg`             | 2 (operators) |
| `/coords`                   | `fleettools.coords`          | 2 (operators) |
| `/daylight-pause`           | `fleettools.daylight`        | 2 (operators) |
| `/day`                      | `fleettools.time`            | 2 (operators) |
| `/night`                    | `fleettools.time`            | 2 (operators) |
| `/sun`                      | `fleettools.weather`         | 2 (operators) |
| `/rain`                     | `fleettools.weather`         | 2 (operators) |
| `/thunderstorm`             | `fleettools.weather`         | 2 (operators) |

## Data Storage

Fleet Tools stores player data in JSON files in the `fleettools` folder within your server directory:

- `fleettools/players/` - Individual player data (homes, last locations, mute status, temporary bans, etc.)
- `fleettools/global.json` - Global server data (spawn location, etc.)
- `fleettools/warps.json` - Warp locations and data

### Automatic Features

- **Location Tracking**: Player locations are automatically saved on disconnect for `/tpo` and `/back` commands
- **Persistent States**: God mode, fly mode, and mute status persist across server restarts
- **Automatic Cleanup**: Expired temporary bans are automatically removed on player join

## Compatibility

- **Minecraft Version**: 1.20.1
- **Fabric Loader**: 0.12.5+
- **Java**: 17+

## Configuration

The mod automatically creates necessary data files and folders on first run. No additional configuration is required.

## Commands Reference

### Basic Usage Examples

```bash
# Teleportation Commands
/home              # Teleport to your home
/sethome           # Set home at current location
/delhome           # Delete your home
/spawn             # Teleport to spawn
/setspawn          # Set spawn (admin only)
/back              # Return to previous location

# Advanced Teleportation
/tpo Steve         # Teleport to Steve (online or offline)
/tpoffline Alex    # Alternative syntax for offline teleportation
/top               # Teleport to highest block above you
/top Steve         # Teleport Steve to highest block above him

# Player Management
/heal              # Heal yourself
/heal Steve        # Heal another player (admin only)
/feed              # Feed yourself
/feed Steve        # Feed another player (admin only)
/fly               # Toggle flight for yourself
/fly Steve         # Toggle flight for another player (admin only)

# Game Mode Commands
/gamemode creative # Change to creative mode
/gmc               # Quick creative mode
/gms               # Quick survival mode
/gma               # Quick adventure mode
/gmsp              # Quick spectator mode
/god               # Toggle god mode

# Warp System
/warp myWarp       # Teleport to a warp named 'myWarp'
/setwarp myWarp    # Set a warp named 'myWarp' at current location
/delwarp myWarp    # Delete the warp named 'myWarp'

# Moderation Commands
/unban Steve       # Remove Steve from ban list (substitute for /pardon)
/mute Steve        # Prevent Steve from sending chat messages
/unmute Steve      # Allow Steve to send chat messages again
/tempban Steve 1h  # Ban Steve for 1 hour
/tempban Steve 1d Griefing  # Ban Steve for 1 day with reason

# Communication
/msg Steve Hello!  # Send private message to Steve

# Utility Commands
/coords Steve      # Show Steve's coordinates and world
/daylight-pause    # Pause or resume daylight cycle

# Time & Weather Commands
/day               # Set time to day (7:00 AM)
/night             # Set time to night (7:00 PM)
/sun               # Set weather to clear/sunny
/rain              # Set weather to rain
/thunderstorm      # Set weather to thunderstorm
```
