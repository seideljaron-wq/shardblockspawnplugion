# ShardShopBlock

Lets players right-click a Shulker Box at spawn to open the **ShardShop** GUI.  
Companion plugin for [ShardSystem](https://github.com/YOUR_USERNAME/ShardSystem).

## Features
- Right-click any registered Shulker Box → opens `/shardshop`
- Floating hologram label above the box (`✦ Shard Shop ✦`)
- Rotating END_ROD particle ring effect
- Explosion-proof & break-proof for normal players
- Supports any Shulker Box color
- Multiple shop blocks possible (e.g. different worlds)

## Requirements
- Java 17+
- Paper / Spigot 1.20+
- **ShardSystem** plugin installed

## Building
```bash
mvn clean package
```
Output: `target/ShardShopBlock-1.0.0.jar`

## Setup (3 steps)
1. Place a Shulker Box at your spawn
2. Look directly at it (within 5 blocks)
3. Run `/shopblock set`

Done! Players can now right-click it to open the shop.

## Commands

| Command | Description |
|---------|-------------|
| `/shopblock set` | Look at a Shulker Box → register it as shop |
| `/shopblock remove` | Look at a Shulker Box → unregister it |
| `/shopblock list` | List all registered shop blocks |

## Permission
| Permission | Default |
|------------|---------|
| `shardshopblock.admin` | OP |

## config.yml
```yaml
show-particles: true          # rotating particle ring above box
hologram-text: "&b✦ Shard Shop ✦"  # floating text (use & color codes)
```
