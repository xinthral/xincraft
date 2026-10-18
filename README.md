# Xincraft

A Minecraft Forge mod that adds tin, quieter rails, and teleporting ender rails.

- **Minecraft:** 26.2
- **Forge:** 65.1.0 or newer
- **Java:** 25

## Contents

- [Tin](#tin)
- [Tin Rails](#tin-rails)
- [Padded Rails](#padded-rails)
- [Padded Powered Rails](#padded-powered-rails)
- [Ender Rails](#ender-rails)
- [Ender Minecart](#ender-minecart)
- [Configuration](#configuration)
- [Building from source](#building-from-source)

All of the mod's items are in the **Xincraft** creative tab.

---

## Tin

| Item | Notes |
| --- | --- |
| Tin Ingot | Also tagged `c:ingots/tin` so other mods recognize it |
| Block of Tin | Storage block; needs a pickaxe to drop |
| Tin Helmet, Chestplate, Leggings, Boots | Armor between copper and iron |

Tin armor gives 2 / 5 / 4 / 1 armor points (helmet / chestplate / leggings / boots), has a durability multiplier of 13 (copper is 11, iron is 15), and an enchantability of 10. Repair it with tin ingots in an anvil.

> Tin currently has no crafting recipes or ore, so it's only available from the creative tab.

## Tin Rails

Fast rails. A minecart on a tin rail can travel **twice as fast** as on any other rail (configurable). Once it rolls back onto ordinary rails it's limited to normal speed again.

| Rail | Recipe | Makes |
| --- | --- | --- |
| Tin Rail | Rail recipe with Tin Ingots in place of Iron Ingots (6 Tin Ingots + 1 Stick) | 16 |
| Tin Powered Rail | Powered Rail recipe with Tin Ingots in place of Gold Ingots (6 Tin Ingots + 1 Stick + 1 Redstone) | 6 |

Tin Powered Rails boost and brake carts just like vanilla powered rails, and pass power along a line that mixes tin and vanilla powered rails. A line of Tin Rails with Tin Powered Rails spaced along it lets carts build up to, and hold, the higher speed.

> The speed boost only works with the default minecart physics. Carts are also more likely to fly off sharp corners at high speed, so give fast lines gentle curves or slow carts down before a turn.

## Padded Rails

Rails with wool under the ties. A minecart rolling over them is much quieter, and the sound fades smoothly as a cart moves on and off a padded section.

**Recipe** (shapeless):

> 2 Wool + 1 Rail → 1 Padded Rail

Both pieces of wool must be the **same color**, and the rail takes on that color. There are 16 colors, one per wool color, from White Padded Rail to Black Padded Rail.

Padded rails work exactly like normal rails: they curve, slope, and connect to any other rail.

## Padded Powered Rails

Powered rails with the same muffling. They boost carts when powered and brake them when unpowered, just like a vanilla powered rail, and power passes along a line that mixes padded and vanilla powered rails.

**Recipe** (shapeless):

> 2 Wool + 1 Powered Rail → 1 Padded Powered Rail

As with padded rails, both wools must be the same color, and there are 16 colors.

## Ender Rails

A pair of linked ender rails teleports a minecart and its rider from one rail to the other, keeping its speed.

**Recipe** (shapeless):

> 2 Powered Rails + 1 Ender Pearl → 2 Ender Rails

### Linking two ender rails

1. **Place** the first ender rail.
2. **Hold** a second ender rail and **look at** the first one. It lights up with purple sparkles when it's ready to link.
3. **Right-click** it. The placed rail and the rail in your hand both glow for a second. The held rail is now *attuned*, and its tooltip shows the coordinates it's attuned to.
4. **Place** the attuned rail where you want the other end. The two rails link and both glow again to confirm.

You can tell whether a rail is linked by its eyes: **dim** means unlinked, **lit** (with a faint glow) means linked.

**Linking rules**

- Both rails must be in the **same dimension**.
- They must be within the configured **maximum distance** (64 blocks by default).
- Each rail links to exactly one other rail. Attuning to a rail that's already linked moves its link to the new rail, and its old partner becomes unlinked.
- Breaking either rail unlinks the other.

### Teleporting

By default only the [Ender Minecart](#ender-minecart) can use ender rails; other minecarts roll over them like ordinary track. This can be changed in the [configuration](#configuration).

When a moving ender minecart rolls onto a linked ender rail:

- It's teleported, with its rider, to the linked rail, keeping its speed.
- It keeps heading the same way if the two rails run the same way. If the destination rail runs across the entry rail, the cart turns **right** onto it, and the rider's view turns with it.
- After teleporting, the cart has a **cooldown** (3 seconds by default) before it can teleport again; during it, ender rails act as ordinary track.
- A cart that has stopped on an ender rail stays put. It only teleports while moving.

To keep servers stable, there's also a server-wide limit on how many teleports can happen per second (20 by default). When the limit is reached, carts roll over ender rails as ordinary track until the next second.

> Teleporting only works with the default minecart physics, not Minecraft's experimental minecart physics.

## Ender Minecart

A rideable minecart on ender wheels, and the only cart that can teleport on ender rails (unless the configuration allows other minecarts). It handles like a normal minecart in every other way.

**Recipe** (shapeless):

> 1 Minecart + 4 Ender Pearls → 1 Ender Minecart

---

## Configuration

Xincraft's settings live in `config/xincraft-common.toml`. The file is created the first time the game starts, with a comment and the allowed range for each value. Changes made while the game is running take effect immediately.

```toml
[paddedRails]
	volume = 0.2

[tinRails]
	speedMultiplier = 2.0

[enderRails]
	maxDistance = 64
	cooldownSeconds = 3.0
	maxTeleportsPerSecond = 20
	allowOtherMinecarts = false
```

| Setting | Default | Range | What it does |
| --- | --- | --- | --- |
| `paddedRails.volume` | `0.2` | 0.0 – 1.0 | Volume of minecart sounds on padded rails (0 = silent, 1 = vanilla volume) |
| `tinRails.speedMultiplier` | `2.0` | 1.0 – 3.0 | How many times faster minecarts can travel on tin rails than on other rails |
| `enderRails.maxDistance` | `64` | 1 – 30,000,000 | Furthest apart, in blocks, two ender rails can be and still link. Lowering it stops existing links that are now too long from teleporting, without unlinking them |
| `enderRails.cooldownSeconds` | `3.0` | 0.0 – 3600.0 | Seconds a cart must wait after teleporting before it can teleport again |
| `enderRails.maxTeleportsPerSecond` | `20` | 0 – 1000 | Most ender rail teleports allowed across the whole server in any one second (0 = no limit) |
| `enderRails.allowOtherMinecarts` | `false` | true / false | Let minecarts other than the Ender Minecart (vanilla carts and other mods' carts) teleport on ender rails |

On a dedicated server, the server's config file controls ender rail behavior. `paddedRails.volume` is read by each player's own game.

---

## Building from source

Requires a JDK 25. Gradle downloads everything else.

| Command | What it does |
| --- | --- |
| `./gradlew build` | Builds the mod jar into `build/libs/` |
| `./gradlew runClient` | Starts the game with the mod loaded (uses the `run/` folder) |
| `./gradlew runServer` | Starts a dedicated server with the mod loaded |
| `./gradlew runGameTestServer` | Starts a headless server, runs the game tests, and exits; a quick way to check that the mod loads cleanly |

On Windows, use `gradlew.bat` in place of `./gradlew`.

### Project layout

| Path | Contents |
| --- | --- |
| `src/main/java/com/xincraft/xincraft/` | `XincraftMod` (entry point) and `Config` |
| `.../registry/` | Registration of blocks, items, block entities, entities, data components, tags, and the creative tab |
| `.../block/` | Powered rail variants (padded, tin) and the ender rail |
| `.../item/` | Ender rail item (attuning and glint) |
| `.../entity/` | Ender minecart, and the minecart physics that speed carts up on tin rails |
| `.../client/` | Client-only code: padded rail sound muffling, ender rail highlight, ender minecart renderer |
| `src/main/resources/assets/xincraft/` | Block states, models, textures, and translations |
| `src/main/resources/data/` | Recipes, loot tables, and tags |
