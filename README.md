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
- Also returns you to your **death location** after you respawn
- Permission: `fleettools.back` (default: operators only)
- Death-return is **on by default**; deny `fleettools.back.ondeath` for a player/group to disable just the death-return behavior

### Teleportation

- **`/tpo <player>`** or **`/tpoffline <player>`** - Teleport to any player's location (online or offline)
- **`/top <player>`** - Teleport to the highest block above current position
- **`/tpall [player]`** - Teleport all online players to you (or to the given player)
- Permission: `fleettools.tpo`, `fleettools.top`, `fleettools.top.others`, `fleettools.tpall` (default: operators only)

### Teleport Requests

Consensual player-to-player teleporting (EssentialsX-style), available to all players by default:

- **`/tpa <player>`** - Request to teleport **to** another player
- **`/tphere <player>`** - Request that another player teleport **to you**
- **`/tpaccept [player]`** - Accept a request — the most recent one, or a specific player's
- **`/tpdeny [player]`** - Deny a request — the most recent one, or a specific player's
- You can hold **multiple incoming requests** at once (one per requester); a new request from the same player replaces their old one. Requests expire after 2 minutes.
- Permission: `fleettools.tpa`, `fleettools.tphere`, `fleettools.tpaccept`, `fleettools.tpdeny` (default: everyone)

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

- **`/keepinv`** - Toggle your own keep inventory (admin)
- **`/keepinv status`** - Check your current keep inventory status
- **`/keepinv <player>`** - Toggle keep inventory for another player (admin)
- Permission: `fleettools.keepinventory` (keep items on death — **granted to operators by default**), `fleettools.keepinv` / `fleettools.keepinv.others` (command access, operators)

Keep inventory is gated by the **`fleettools.keepinventory`** permission, which operators have by default. Grant it to other players/groups (or deny it) through a permissions manager like LuckPerms. For servers **without** a permissions manager, the `/keepinv` command sets a per-player override that takes precedence — so admins can grant or revoke keep-inventory in-game. Players still lose XP when they die, maintaining some consequence for death.

This system works with modded inventories, trinkets, and backpacks, ensuring comprehensive item protection regardless of which inventory expansion mods are installed. **AFK players are fully supported** - inventory data is stored persistently and will be restored even if players don't respawn for extended periods or disconnect while dead.

### God Mode

- **`/god [player]`** - Toggle invulnerability
- Permission: `fleettools.god`, `fleettools.god.others` (default: operators only)

### Warps System

- **`/warp`** - List all available warps
- **`/warp <name>`** - Teleport to a named warp location
- **`/setwarp <name>`** - Set a warp at your current location
- **`/delwarp <name>`** - Delete a named warp
- Permission: `fleettools.warp`, `fleettools.setwarp`, `fleettools.delwarp` (default: operators only)

### Moderation Tools

- **`/ban <player> [reason]`** - Permanently ban a player (online or offline) and kick them if online
- **`/ban-ip <ip|player> [reason]`** - Ban an IP address (or an online player's IP) and kick anyone connected from it
- **`/banlist [players|ips]`** - List banned players and/or IP addresses
- **`/unban <player>`** - Remove player from ban list (substitute for /pardon)
- **`/mute <player>`** - Prevent player from sending chat messages
- **`/unmute <player>`** - Allow muted player to send chat messages again
- **`/tempban <player> <time> [reason]`** - Temporarily ban player with automatic expiry
  - Time formats: `30s`, `5m`, `2h`, `1d`, `7d`, etc.
- **`/kick <player> [reason]`** - Disconnect an online player, with an optional reason shown to them
- Permission: `fleettools.ban`, `fleettools.banip`, `fleettools.banlist`, `fleettools.unban`, `fleettools.mute`, `fleettools.unmute`, `fleettools.tempban`, `fleettools.kick` (default: level 3)

### Kill

- **`/kill`** - Kill yourself
- **`/kill <player>`** - Kill another online player
- Permission: `fleettools.kill`, `fleettools.kill.others` (default: operators only)

### Communication

- **`/msg <player> <message>`** - Send private message to player with actionbar display (also `/tell`, `/w`)
- **`/broadcast <message>`** - Send a server-wide announcement to all players (alias: `/bc`)
- Permission: `fleettools.msg`, `fleettools.broadcast` (default: operators only)

### Utility Commands

- **`/coords`** - Display your own coordinates and world information (available to everyone by default)
- **`/coords <player>`** - Display another player's coordinates (operators/mods only)
- **`/daylight-pause`** - Pause or resume the daylight cycle
- Permission: `fleettools.coords` (self, default: everyone), `fleettools.coords.others` (others, default: operators), `fleettools.daylight` (default: operators only)

### Time & Weather Control

- **`/day`** - Set time to day (7:00 AM)
- **`/night`** - Set time to night (7:00 PM)
- **`/sun`** - Set weather to clear/sunny
- **`/rain`** - Set weather to rain
- **`/thunderstorm`** - Set weather to thunderstorm
- Permission: `fleettools.time`, `fleettools.weather` (default: operators only)

## Installation

1. Make sure you are running **Minecraft 26.2** with **Java 25+** and **Fabric Loader 0.19.3+**
2. Download the latest release from the releases page
3. Place the mod file in your `mods` folder
4. Install the required dependencies:
   - Fabric API (0.153.0+26.2 or newer)
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
| `/back` (death-return)      | `fleettools.back.ondeath`    | on by default |
| `/tpo <player>`             | `fleettools.tpo`             | 2 (operators) |
| `/top`                      | `fleettools.top`             | 2 (operators) |
| `/top <player>`             | `fleettools.top.others`      | 2 (operators) |
| `/tpall`                    | `fleettools.tpall`           | 2 (operators) |
| `/tpa`                      | `fleettools.tpa`             | everyone      |
| `/tphere`                   | `fleettools.tphere`          | everyone      |
| `/tpaccept`                 | `fleettools.tpaccept`        | everyone      |
| `/tpdeny`                   | `fleettools.tpdeny`          | everyone      |
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
| `/ban`                      | `fleettools.ban`             | 3 (admins)    |
| `/ban-ip`                   | `fleettools.banip`           | 3 (admins)    |
| `/banlist`                  | `fleettools.banlist`         | 3 (admins)    |
| `/kick`                     | `fleettools.kick`            | 3 (admins)    |
| `/kill`                     | `fleettools.kill`            | 2 (operators) |
| `/kill <player>`            | `fleettools.kill.others`     | 2 (operators) |
| `/msg` `/tell` `/w`         | `fleettools.msg`             | 2 (operators) |
| `/broadcast` `/bc`          | `fleettools.broadcast`       | 3 (admins)    |
| `/coords` (self)            | `fleettools.coords`          | everyone      |
| `/coords <player>`          | `fleettools.coords.others`   | 2 (operators) |
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

- **Minecraft Version**: 26.2
- **Fabric Loader**: 0.19.3+
- **Fabric API**: 0.153.0+26.2 or newer
- **Java**: 25+

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
