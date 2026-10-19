# Xincraft

A Minecraft Forge mod that adds tin and bronze, plus a set of rails: fast tin rails, quiet padded rails, speed-limiting honey rails, launching bouncy rails, cart-sorting inspector rails, and teleporting ender rails. It also adds minecarts: a self-driving locomotive cart and a music-playing jingle cart, with upgrades.

- **Minecraft:** 26.2
- **Forge:** 65.1.0 or newer
- **Java:** 25

## Contents

- [Tin](#tin)
- [Bronze](#bronze)
- [Tin Rails](#tin-rails)
- [Padded Rails](#padded-rails)
- [Padded Powered Rails](#padded-powered-rails)
- [Honey Rail](#honey-rail)
- [Bouncy Rail](#bouncy-rail)
- [Inspector Rail](#inspector-rail)
- [Ender Rails](#ender-rails)
- [Minecart Frame](#minecart-frame)
- [Ender Minecart](#ender-minecart)
- [Locomotive Cart](#locomotive-cart)
- [Jingle Cart](#jingle-cart)
- [Cart Upgrades](#cart-upgrades)
- [Tin Spyglass](#tin-spyglass)
- [Configuration](#configuration)
- [Building from source](#building-from-source)

All of the mod's items are in the **Xincraft** creative tab.

---

## Tin

| Item | Notes |
| --- | --- |
| Tin Ore, Deepslate Tin Ore | Generate underground in the Overworld; need a stone pickaxe or better |
| Raw Tin | Dropped by tin ore |
| Tin Ingot | Smelted from raw tin or tin ore |
| Block of Tin | Storage block for 9 tin ingots; needs a stone pickaxe or better |
| Tin Helmet, Chestplate, Leggings, Boots | Armor between copper and iron |

### Finding tin

Tin ore generates in every Overworld biome, a little less often than copper: up to 12 veins of up to 9 blocks per chunk, between Y −32 and Y 96, most common around Y 32. Below the stone layer it appears as Deepslate Tin Ore.

If another mod in your pack already generates tin, you can turn this mod's tin ore off with `tin.generateOre` in `config/xincraft-common.toml`. The change takes effect the next time a world is loaded, and only in chunks generated after that.

Mining tin ore drops one Raw Tin, more with Fortune; Silk Touch drops the ore block itself.

### Recipes

| Result | Recipe |
| --- | --- |
| Tin Ingot | Smelt or blast Raw Tin, Tin Ore, or Deepslate Tin Ore |
| Block of Tin | 9 Tin Ingots |
| 9 Tin Ingots | 1 Block of Tin |
| Tin Helmet, Chestplate, Leggings, Boots | Same shapes as iron armor, with Tin Ingots (5 / 8 / 7 / 4 ingots) |

Tin armor gives 2 / 5 / 4 / 1 armor points (helmet / chestplate / leggings / boots), has a durability multiplier of 13 (copper is 11, iron is 15), and an enchantability of 10. Repair it with tin ingots in an anvil.

Tin armor takes **armor trims** like vanilla armor: any trim pattern in any trim material, at a smithing table. The trim shows on the armor when worn and on its inventory icon.

Tin ingots are also a **trim material**, giving a dark, almost black gray trim on any armor (darker still on tin armor). For the trim to show on vanilla armor's inventory icons, the mod adds a tin case to vanilla's armor item model files (`assets/minecraft/items/`). A resource pack or mod that replaces those files hides tin trims on the icons only; they still show when worn. Only this mod's tin ingot works as the material, since each trim material comes from a specific item.

**Full set bonus:** wearing all four pieces of tin armor gives **Fire Resistance**, 5 seconds at a time, renewed every 5 seconds while the full set stays on. Take any piece off and it wears off within 5 seconds. Longer Fire Resistance, such as from a potion, isn't cut short.

Tin is tagged with the common `c:` tags (`c:ingots/tin`, `c:ores/tin`, `c:raw_materials/tin`, `c:storage_blocks/tin`), so other mods' machines and recipes recognize it. It works the other way too: every recipe here that uses tin, and repairing tin armor, accepts tin from any mod that uses these tags.

### Reinforced Armor Trim

A dark edging along every edge of the armor, like metal banding on reinforced gear, in a deep shade of the trim material's color. It pairs especially well with tin.

The **Reinforced Armor Trim Smithing Template** is found in abandoned mineshaft chests (10% of chests). Duplicate it like a vanilla template: 7 Diamonds around the template and a Cobblestone makes 2.

## Bronze

An alloy of copper and tin.

| Item | Recipe | Makes |
| --- | --- | --- |
| Raw Bronze | 3 Raw Copper + 1 Raw Tin, shapeless | 4 |
| Bronze Ingot | Smelt or blast Raw Bronze | 1 |
| Block of Bronze | 9 Bronze Ingots (and back into 9 ingots) | 1 |
| Bronze Helmet, Chestplate, Leggings, Boots | Same shapes as iron armor, with Bronze Ingots (5 / 8 / 7 / 4 ingots) | 1 |

Bronze is a muted golden orange, and the Block of Bronze has copper's polished shine. It needs a stone pickaxe or better to mine. Bronze uses the common `c:` tags (`c:raw_materials/bronze`, `c:ingots/bronze`, `c:storage_blocks/bronze`), and the raw bronze recipe accepts any mod's raw copper and raw tin.

Bronze armor gives the same protection as iron, 2 / 6 / 5 / 2 armor points (helmet / chestplate / leggings / boots), but lasts longer, with a durability multiplier of 17 (iron is 15), and has an enchantability of 12. Repair it with bronze ingots in an anvil. It takes armor trims like vanilla armor.

**Full set bonus:** wearing all four pieces of bronze armor lets you **walk straight up one-block steps** without jumping, the way horses do (step height 1 block instead of 0.6). Take any piece off and it's back to normal at once.

## Tin Rails

Fast rails. A minecart on straight tin rails, slopes included, can travel **twice as fast** as on any other rail (configurable). Once it rolls back onto ordinary rails it's limited to normal speed again.

| Rail | Recipe | Makes |
| --- | --- | --- |
| Tin Rail | Rail recipe with Tin Ingots in place of Iron Ingots (6 Tin Ingots + 1 Stick) | 16 |
| Tin Powered Rail | Powered Rail recipe with Tin Ingots in place of Gold Ingots (6 Tin Ingots + 1 Stick + 1 Redstone) | 6 |

Tin Powered Rails boost and brake carts just like vanilla powered rails, and pass power along a line that mixes tin and vanilla powered rails. A line of Tin Rails with Tin Powered Rails spaced along it lets carts build up to, and hold, the higher speed.

Carts automatically ease down to normal speed for corners, starting one block before the curve, so they don't fly off the outside of the turn. They speed back up on the next straight without needing another powered rail. Hills don't slow them: carts climb and descend at full tin speed.

> The speed boost only works with the default minecart physics.

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

## Honey Rail

A speed-limiting rail. A minecart on a honey rail is held at **half its normal top speed** (configurable): a faster cart is slowed straight down to it, and a slower cart is pushed up to it, so carts keep moving instead of stopping. No redstone is needed. Use them before sharp corners, through stations, or anywhere carts should take it slow.

**Recipe** (shaped):

| | | |
| --- | --- | --- |
| Honey Bottle | Tin Powered Rail | Honey Bottle |
| Honey Block | Tin Powered Rail | Honey Block |
| Honey Bottle | Tin Powered Rail | Honey Bottle |

→ 3 Honey Rails. The empty glass bottles are given back.

Like powered rails, honey rails can slope but not curve. A cart that's completely stopped on a honey rail stays put; give it a push and it'll carry on at the slower speed.

> The speed handling only works with the default minecart physics.

## Bouncy Rail

A launch rail. When it's powered, a minecart rolling over it is thrown into the air and keeps its forward speed, so it sails over a gap or up onto a higher track. The **strength of the redstone signal** sets the height. At full power (15) a cart rises about **8 blocks** (configurable), and each level lower takes off about half a block less. With no power it's an ordinary rail.

**Recipe** (shaped):

| | | |
| --- | --- | --- |
| Slimeball | Tin Powered Rail | Slimeball |
| Slime Block | Tin Powered Rail | Slime Block |
| Slimeball | Tin Powered Rail | Slimeball |

→ 3 Bouncy Rails.

Any signal strength works, so you can set the height with a comparator, a target block, or redstone dust run a few blocks before it reaches the rail. The rail's slime pads light up whenever it's powered. Unlike powered rails, a bouncy rail doesn't pass power on to the next one; each rail reads its own signal.

On the way up, a cart ignores any rails it passes. From the top of the arc it can land on rails again. A cart that's stopped on a bouncy rail stays put rather than bouncing in place. Like other flying minecarts, a launched cart slowly loses speed to air drag.

> The launch only works with the default minecart physics.

## Inspector Rail

A detector rail that tells minecarts apart. Instead of always giving a full-strength signal, it outputs a redstone signal whose strength depends on the type of minecart on it, so a comparator or a line of redstone dust can sort carts.

**Recipe** (shapeless):

> 1 Detector Rail + 1 Comparator → 1 Inspector Rail

| Minecart | Signal |
| --- | --- |
| Minecart | 1 |
| Minecart with Hopper | 3 |
| Minecart with Chest, Minecart with Furnace | 6 |
| Locomotive Cart | 9 |
| Ender Minecart | 12 |
| Minecart with TNT | 15 |

Any other minecart (for example from another mod) gives 0, the same as an empty rail, until it's added to one of the signal tags below. If several carts are on the rail at once, it outputs the strongest signal. Like the detector rail, it strongly powers the block beneath it, powers adjacent powered rails, keeps its signal for up to a second after a cart leaves, and lets a comparator read how full a container cart on it is.

**Adding cart types:** the signal for each cart type comes from the entity type tags `xincraft:inspector_rail_signal/1` through `/15`, in `data/xincraft/tags/entity_type/inspector_rail_signal/`. To give a new cart a signal, add its ID to the tag file for that strength (creating the file if it doesn't exist yet). A datapack can do the same to add carts from other mods.

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

## Minecart Frame

A bare tin minecart frame, the base for the mod's special minecarts.

**Recipe** (shaped, in the vanilla minecart's shape):

| | | |
| --- | --- | --- |
| Tin Ingot | | Tin Ingot |
| Tin Ingot | Tin Ingot | Tin Ingot |

> 5 Tin Ingots → 1 Minecart Frame

## Ender Minecart

A rideable minecart on ender wheels, and the only cart that can teleport on ender rails (unless the configuration allows other minecarts). It handles like a normal minecart in every other way.

**Recipe** (shapeless):

> 1 Minecart Frame + 4 Ender Pearls → 1 Ender Minecart

## Locomotive Cart

A rideable minecart with its own engine. While it has fuel burning, it drives itself forward, like a furnace minecart you can sit in.

**Recipe** (shapeless):

> 1 Minecart Frame + 1 Blast Furnace + 1 Chest → 1 Locomotive Cart

### Inventory

The cart has two slots:

- **Fuel slot**, with a furnace-style flame above it showing how much of the current fuel is left. It takes anything that burns in a furnace, and each item burns **8 times as long** as it would in a furnace (configurable). A lava bucket leaves its empty bucket behind, just like in a furnace.
- **Storage slot**, a single slot for carrying anything.

Ways to open it:

- **Shift + right-click** the cart.
- **Press your inventory key** while riding it (like a chest boat or donkey).

**Hoppers** above the track fill the cart as it passes underneath, like chest and hopper minecarts. Fuel goes into the fuel slot first; anything else, or extra fuel once the fuel slot is full, goes into the storage slot. Hoppers can't pull fuel back out of the cart (only empty buckets).

### Driving

- **Right-click** to get in.
- **Getting going:** while stopped, press a movement key to set off in that direction. A cart without a rider sets off when pushed.
- **Staying going:** once moving, it keeps driving whichever way it's going, around curves and up slopes, for as long as there's fuel.
- **Stopping:** an **unpowered powered rail** brings it to a stop, the same way it stops any minecart. Power the rail to set off again. It also stops if it runs into something or reaches the end of the track.
- **Fuel use:** a new fuel item is only lit when the cart is trying to move, so a parked locomotive doesn't eat its fuel. An item that's already burning keeps burning down while parked, like in a furnace.
- **Speed:** it drives at the normal minecart speed cap, at the higher speed on straight [Tin Rails](#tin-rails), and at the slower speed on [Honey Rails](#honey-rail).

> Like the other powered features, the engine only works with the default minecart physics.

## Jingle Cart

A minecart carrying a jukebox that plays music discs as it rolls along, the music following the cart down the track.

**Recipe** (shapeless):

> 1 Minecart Frame + 1 Jukebox + 1 Tin Ingot + 1 Amethyst Shard → 1 Jingle Cart

- **Right-click with a music disc** to put it in; **right-click again** to take it out. Any music disc works, including other mods'.
- **It plays while it's moving.** The song starts when the cart sets off and stops shortly after it comes to a halt. A song can't be resumed partway, so it starts from the top each time the cart sets off again. As long as it keeps rolling, the song loops.
- It's as loud as a jukebox and uses the **Jukebox/Note Blocks** volume slider.
- If you come within earshot partway through a song, you'll hear the cart from the next time it starts the song.
- Breaking the cart drops the disc.

## Cart Upgrades

Upgrades fit onto the Xincraft minecarts (ender minecart, locomotive cart, and jingle cart) and show as small blocks on the cart's rim.

- **Fit an upgrade** by right-clicking the cart with it. Each upgrade can be fitted once per cart.
- **Remove upgrades** with **shears**: each right-click snips off the last one fitted and drops it.
- Breaking the cart drops its upgrades.

| Upgrade | Recipe | Fits | Effect |
| --- | --- | --- | --- |
| Shrieker Upgrade | Sculk Shrieker surrounded by 4 Tin Ingots (one on each side) | Any Xincraft cart | Clears the track ahead: mobs and players in the cart's path are shoved off to the side with a shriek before the cart reaches them. The cart never picks them up and is never slowed by them (it still bumps into other minecarts). |
| Sculk Sensor Upgrade | Sculk Sensor surrounded by 4 Tin Ingots (one on each side) | Jingle Cart | The cart only plays while a player is within 16 blocks. |

> The shrieker, like the other powered features, only works with the default minecart physics.

## Tin Spyglass

A spyglass with a night-vision lens. Looking through it lets you see in the dark as if you had Night Vision, and tints the view green. The effect stops as soon as you lower it, and it never shows an effect icon. If you already have Night Vision from a potion, the spyglass leaves it alone.

**Recipe** (shaped, the spyglass recipe with Tin Ingots in place of Copper Ingots):

| | | |
| --- | --- | --- |
| | Amethyst Shard | |
| | Tin Ingot | |
| | Tin Ingot | |

→ 1 Tin Spyglass

It zooms, sounds, and counts for advancements exactly like a normal spyglass, because it is one: a vanilla spyglass marked with the `xincraft:tin_lens` component. To give it its tin look, the mod replaces the vanilla spyglass's item model definition (`assets/minecraft/items/spyglass.json`) with one that picks the tin model when that component is set. A resource pack that retextures the spyglass can override this, in which case the tin spyglass looks like a normal one but still works.

---

## Configuration

Xincraft's settings live in `config/xincraft-common.toml`. The file is created the first time the game starts, with a comment and the allowed range for each value. Changes made while the game is running take effect immediately, except `tin.generateOre` (see below).

```toml
[tin]
	generateOre = true

[paddedRails]
	volume = 0.2

[tinRails]
	speedMultiplier = 2.0

[honeyRails]
	speedMultiplier = 0.5

[bouncyRails]
	maxLaunchHeight = 8.0

[locomotiveCart]
	fuelMultiplier = 8.0

[enderRails]
	maxDistance = 64
	cooldownSeconds = 3.0
	maxTeleportsPerSecond = 20
	allowOtherMinecarts = false
```

| Setting | Default | Range | What it does |
| --- | --- | --- | --- |
| `tin.generateOre` | `true` | true / false | Whether tin ore generates in the Overworld. Turn it off if another mod already generates tin. Takes effect the next time a world is loaded, and only in chunks generated after that |
| `paddedRails.volume` | `0.2` | 0.0 – 1.0 | Volume of minecart sounds on padded rails (0 = silent, 1 = vanilla volume) |
| `tinRails.speedMultiplier` | `2.0` | 1.0 – 3.0 | How many times faster minecarts can travel on straight tin rails (slopes included) than on other rails |
| `honeyRails.speedMultiplier` | `0.5` | 0.05 – 1.0 | Speed minecarts are held at on honey rails, as a fraction of their normal top speed |
| `bouncyRails.maxLaunchHeight` | `8.0` | 1.0 – 64.0 | Roughly how many blocks high a bouncy rail launches a cart at full redstone power (15). Weaker signals launch proportionally lower |
| `locomotiveCart.fuelMultiplier` | `8.0` | 1.0 – 64.0 | How many times longer fuel burns in a locomotive cart than in a furnace |
| `enderRails.maxDistance` | `64` | 1 – 30,000,000 | Furthest apart, in blocks, two ender rails can be and still link. Lowering it stops existing links that are now too long from teleporting, without unlinking them |
| `enderRails.cooldownSeconds` | `3.0` | 0.0 – 3600.0 | Seconds a cart must wait after teleporting before it can teleport again |
| `enderRails.maxTeleportsPerSecond` | `20` | 0 – 1000 | Most ender rail teleports allowed across the whole server in any one second (0 = no limit) |
| `enderRails.allowOtherMinecarts` | `false` | true / false | Let minecarts other than the Ender Minecart (vanilla carts and other mods' carts) teleport on ender rails |

On a dedicated server, the server's config file controls everything except `paddedRails.volume`, which is read by each player's own game.

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
| `.../registry/` | Registration of blocks, items, block entities, entities, menus, data components, biome modifier types, tags, the tin trim material, and the creative tab |
| `.../block/` | Powered rail variants (padded, tin), the honey, bouncy, inspector, and ender rails |
| `.../item/` | Ender rail item (attuning and glint), tin and bronze armor set bonuses, tin spyglass night vision, cart upgrade items |
| `.../entity/` | Ender minecart, locomotive cart, jingle cart, cart upgrades (`CartUpgrades`), and the minecart physics behind tin, honey, and bouncy rails (`XincraftMinecartBehavior`) |
| `.../world/` | The biome modifier that adds tin ore, unless turned off in the config, and the reinforced trim template's mineshaft loot |
| `.../inventory/` | Locomotive cart menu (slots and burn progress) |
| `.../client/` | Client-only code: padded rail sound muffling, ender rail highlight, minecart renderers (with upgrades), locomotive cart screen, jingle cart music, tin spyglass tint |
| `src/main/resources/assets/xincraft/` | Block states, models, textures, and translations |
| `src/main/resources/data/` | Recipes, loot tables, tags, and tin ore generation (`xincraft/worldgen/`, `xincraft/forge/biome_modifier/`) |
