# Xincraft

A Minecraft Forge mod that adds tin and bronze, plus a set of rails: fast tin rails, quiet padded rails, speed-limiting honey rails, launching bouncy rails, cart-sorting inspector rails, teleporting ender rails, and bronze charging rails, with heat and steam turbines to power them. It also adds minecarts: a self-driving locomotive cart and a music-playing jingle cart, with upgrades, and exotic flowers that give off area effects.

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
- [Charging Rail](#charging-rail)
- [Turbines](#turbines)
- [Minecart Frame](#minecart-frame)
- [Ender Minecart](#ender-minecart)
- [Locomotive Cart](#locomotive-cart)
- [Jingle Cart](#jingle-cart)
- [Cart Upgrades](#cart-upgrades)
- [Tin Spyglass](#tin-spyglass)
- [Exotic Flowers](#exotic-flowers)
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

## Charging Rail

A bronze powered rail that stores Forge Energy (FE) for locomotive carts with the [Energy Upgrade](#energy-upgrade). It doesn't make energy itself: power it with a [Turbine](#turbines), or with another mod's generators.

| Recipe | Makes |
| --- | --- |
| Powered Rail recipe with Bronze Ingots in place of Gold Ingots (6 Bronze Ingots + 1 Stick + 1 Redstone) | 6 |

- **Filling it:** place a [Heat or Steam Turbine](#turbines), or another mod's generator, battery or energy cell, **right next to it** (beside, above or below), and the rail draws energy from it by itself. Cables connected to it work too. It holds up to **50,000 FE** and takes in up to 1,000 FE per tick.
- **Charging a cart:** the rail passes up to 1,000 FE per tick into a locomotive cart with the energy upgrade sitting on it or rolling over it. It also charges other mods' carts that take Forge Energy.
- **It's also a powered rail:** unpowered, it brakes carts and holds them still, like any powered rail; powered, it boosts them. So a charging rail with a generator beside it makes a **charging station**: the locomotive rolls onto it, stops, and charges, and powering the rail (with a lever or button) sends it on its way. It shares power along a line with other powered rails.
- Other mods' machines can draw energy back out of it too.

## Turbines

Generators that burn furnace fuel into Forge Energy (FE), to power [Charging Rails](#charging-rail) without needing another mod (they work with other mods' cables and machines too).

| Turbine | Recipe |
| --- | --- |
| Heat Turbine | Furnace in the middle, Redstone Dust below it, and 7 Tin Ingots around the rest |
| Steam Turbine | Heat Turbine in the middle, Cauldron below it, and 7 Bronze Ingots around the rest |

| | Heat Turbine | Steam Turbine |
| --- | --- | --- |
| Fuel burns | At furnace speed | Twice as fast |
| Energy made | 40 FE per tick | 160 FE per tick |
| Energy per coal | 64,000 FE | 128,000 FE (twice as much) |
| Energy buffer | 50,000 FE | 100,000 FE |
| Water | None | 5 mB per tick (a bucket lasts 10 seconds); 8,000 mB tank |

- **Fuel:** right-click to open it and put any furnace fuel in the fuel slot. **Hoppers** fill it too, from above or the sides. A lava bucket leaves its empty bucket behind, which a hopper underneath can pull out.
- **Output:** it pushes up to 1,000 FE per tick into each neighbouring block that takes energy, so a turbine **next to a charging rail** keeps the rail charged. Other mods' cables can draw from it as well, and a turbine placed **under the track** charges a locomotive passing over it directly.
- **Full buffer:** once the buffer is full, the turbine stops lighting new fuel, so it doesn't eat through the fuel slot while nothing's drawing power. A fuel item that's already burning finishes burning, like in a furnace, and any energy it makes that won't fit is lost.
- **Water (steam turbine):** right-click it with a **water bucket** to fill the tank, or pipe water in from another mod from any side. Without water it stops, and doesn't use fuel.
- **Screen:** the flame shows how much of the current fuel is left, the red bar on the right the stored energy, and (steam turbine) the blue bar on the left the water. Hover over a bar for the exact amount.
- The energy per tick and water use can be changed in the config (under `turbines`).

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

With the [Energy Upgrade](#energy-upgrade) fitted, the screen also shows a red energy bar left of the fuel slot.

Ways to open it:

- **Shift + right-click** the cart.
- **Press your inventory key** while riding it (like a chest boat or donkey).

**Hoppers** above the track fill the cart as it passes underneath, like chest and hopper minecarts. Fuel goes into the fuel slot first; anything else, or extra fuel once the fuel slot is full, goes into the storage slot. Hoppers can't pull fuel back out of the cart (only empty buckets).

### Driving

- **Right-click** to get in.
- **Getting going:** while stopped, press a movement key to set off in that direction. A cart without a rider sets off when pushed.
- **Staying going:** once moving, it keeps driving whichever way it's going, around curves and up slopes, for as long as there's fuel (or stored energy, with the [Energy Upgrade](#energy-upgrade)).
- **Stopping:** an **unpowered powered rail** brings it to a stop, the same way it stops any minecart. Power the rail to set off again. It also stops if it runs into something or reaches the end of the track.
- **Fuel use:** a new fuel item is only lit when the cart is trying to move, so a parked locomotive doesn't eat its fuel. An item that's already burning keeps burning down while parked, like in a furnace. (The [Energy Upgrade](#energy-upgrade) changes this: fuel charges the battery, and the battery drives the cart.)
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
- **Hoppers load and unload it**, like a chest minecart: a hopper feeding into the rail puts a disc in, and a hopper under the rail takes it out. It only takes discs, one at a time.
- On a **detector rail**, a comparator reads a full signal when it has a disc in, and none when it's empty.
- Breaking the cart drops the disc.

## Cart Upgrades

Upgrades fit onto the Xincraft minecarts (ender minecart, locomotive cart, and jingle cart) and show as small blocks on the cart's rim.

> [!IMPORTANT]
> **Removing upgrades: use shears.**
> Hold **shears** and **right-click the cart**. Each click snips off **one** upgrade, the **most recently fitted** first, and puts it back in your inventory (a message above the hotbar says which). With two fitted, click once for the newer one and again for the other. An empty hand or shift + right-click won't remove them.

- **Fit an upgrade** by right-clicking the cart with it. Each upgrade can be fitted once per cart, and only on the carts it fits (see the table); otherwise a message says why.
- Breaking the cart drops its upgrades.

| Upgrade | Recipe | Fits | Effect |
| --- | --- | --- | --- |
| Shrieker Upgrade | Sculk Shrieker surrounded by 4 Tin Ingots (one on each side) | Any Xincraft cart | Clears the track ahead: mobs and players in the cart's path are shoved off to the side with a shriek before the cart reaches them. The cart never picks them up and is never slowed by them (it still bumps into other minecarts). |
| Sculk Sensor Upgrade | Sculk Sensor surrounded by 4 Tin Ingots (one on each side) | Jingle Cart | The cart only plays while a player is within 32 blocks. |
| Energy Upgrade | Block of Redstone surrounded by 4 Tin Ingots (one on each side) | Locomotive Cart | The cart stores Forge Energy (FE) and runs on it, only burning fuel once the energy runs out. See below. |

> The shrieker, like the other powered features, only works with the default minecart physics.

### Energy Upgrade

For modpacks with Forge Energy (FE) mods. A locomotive cart with the energy upgrade holds up to **100,000 FE** and uses **20 FE per tick** while driving, about 4 minutes of driving on a full charge.

- **Charging:** the cart draws up to 1,000 FE per tick from a [Charging Rail](#charging-rail) it's on, which makes a charging station. It also draws from an energy block (a battery, energy cell, generator and so on) **under the rail** it's on, the way a hopper under the rail empties a chest minecart, from other mods' charging rails, and from anything else that hands energy to entities. Energy can't be drawn back out of the cart.
- **Fuel charges the battery:** whenever the battery isn't full, the cart burns fuel from its fuel slot, moving or parked, and every tick of burning goes into the battery (20 FE by default, the same as a tick of driving uses). So fuel lasts just as long as it would without the upgrade. Once the battery is full it stops lighting new fuel, so a parked locomotive with fuel loaded tops itself up and then waits.
- **The battery drives the cart:** with the upgrade, the engine runs only on stored energy, throwing off sparks (and smoke while fuel is burning). While moving with fuel loaded, the fuel refills the battery as fast as driving drains it; once the fuel runs out, the battery keeps it going until it's empty.
- **Energy bar:** the cart's inventory screen shows the charge as a red bar left of the fuel slot. Hover over it for the exact amount.
- The stored energy stays with the cart if the upgrade is removed, but the cart can't use it or charge until the upgrade is fitted again. Breaking the cart loses it.
- The capacity and energy per tick can be changed in the config (`energyCapacity` and `energyPerTick` under `locomotiveCart`).

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

## Exotic Flowers

Flowers that work like a small beacon, each giving off its own effect around it. You grow them from vanilla flowers using **Exotic Pollen**, which you mix on a **Mixing Tray**.

### Mixing Tray

**Recipe:** 5 wooden slabs (any kind) in a U shape (`S S` over `SSS`) → 1 Mixing Tray.

It fills up like a composter:

1. **Right-click it with a flower** to mash it in. Each flower raises the layer of green, petal-flecked flower mash a step. Any small flower works (dandelions, poppies, tulips, lilies of the valley, and so on), but each one must be a **different** kind.
2. **Four different flowers** fill the tray.
3. **Add a honeycomb** to the full tray, either by right-clicking with it or by **throwing it on**. The mash turns into shimmering **Exotic Pollen**.
4. **Right-click with an empty hand** to take the pollen (1 per batch). The tray is then empty and ready for the next batch.

- Flowers can't be taken back out once they're mashed in.
- Breaking the tray drops the pollen if it's finished. A half-made mash is lost, like compost.

### Growing exotic flowers

Right-click one of these flowers with Exotic Pollen to turn it into an exotic flower. Each one uses up one pollen.

| Use pollen on | To grow | Effect |
| --- | --- | --- |
| Lily of the Valley | **Mending Lilac** | **Regeneration I** for players, pets and peaceful mobs within 8 blocks (a 16×16×16 area), but not hostile mobs |
| Dandelion | **Firefly Petunia** | Glows a soft yellow (light level 10) with fireflies drifting around it |
| Golden Dandelion | **Moonlit Dandelion** | **Night Vision** for players within 8 blocks (a 16×16×16 area) |

- **Mending Lilac:** like a beacon, it refreshes the effect every 2 seconds, so it stays on while you're in range and wears off a few seconds after you leave. Several lilacs don't make it stronger. The range can be changed in the config (`flowers.mendingLilacRange`).
- **Moonlit Dandelion:** a pale silver bloom that sheds the odd glimmer of light. Like the lilac, it refreshes Night Vision every 2 seconds. Each dose lasts 15 seconds, so the screen doesn't flicker as it runs low, and it wears off about 15 seconds after you leave. The range can be changed in the config (`flowers.moonlitDandelionRange`).
- **Firefly Petunia:** bright enough to stop mobs spawning around it, while still giving a garden a soft glow.
- Both are planted and broken like any flower (they break instantly and drop themselves), bees pollinate them, and they work in suspicious stew. They also craft into dye: a Mending Lilac makes Magenta Dye, a Firefly Petunia Yellow Dye and a Moonlit Dandelion Light Gray Dye.
- They can't go in flower pots yet.

### Glowing leaves

Right-click any **leaves** with Exotic Pollen to make them glow faintly, about as bright as glow lichen (light level 7). It uses one pollen per block, and works on every kind of vanilla leaves: oak, spruce, birch, jungle, acacia, cherry, dark oak, pale oak, mangrove, azalea and flowering azalea.

Glowing leaves look exactly like the leaves they came from and behave the same way. They decay when their tree is cut down, and drop the same saplings, sticks and apples. Broken with shears or Silk Touch, they drop ordinary leaves, so the glow stays where it was applied and doesn't come with you.

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
	energyCapacity = 100000
	energyPerTick = 20

[flowers]
	mendingLilacRange = 8
	moonlitDandelionRange = 8

[turbines]
	heatTurbineEnergyPerTick = 40
	steamTurbineEnergyPerTick = 160
	steamTurbineWaterPerTick = 5

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
| `locomotiveCart.energyCapacity` | `100000` | 1,000 – 100,000,000 | How much Forge Energy (FE) a locomotive cart with the energy upgrade can store |
| `locomotiveCart.energyPerTick` | `20` | 1 – 100,000 | How much Forge Energy (FE) a locomotive cart with the energy upgrade uses each tick while driving |
| `flowers.mendingLilacRange` | `8` | 1 – 32 | How far, in blocks, a mending lilac's regeneration reaches in each direction (8 = a 16-block-wide area) |
| `flowers.moonlitDandelionRange` | `8` | 1 – 32 | How far, in blocks, a moonlit dandelion's night vision reaches in each direction (8 = a 16-block-wide area) |
| `turbines.heatTurbineEnergyPerTick` | `40` | 1 – 100,000 | Forge Energy (FE) a heat turbine makes each tick while burning fuel (fuel burns at furnace speed) |
| `turbines.steamTurbineEnergyPerTick` | `160` | 1 – 100,000 | Forge Energy (FE) a steam turbine makes each tick while burning fuel (fuel burns at twice furnace speed) |
| `turbines.steamTurbineWaterPerTick` | `5` | 0 – 1,000 | Water, in millibuckets (1,000 = one bucket), a steam turbine uses each tick while running |
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
| `./gradlew build` | Builds the mod jar into `build/libs/`, named with the mod version |
| `./gradlew runClient` | Starts the game with the mod loaded (uses the `run/` folder) |
| `./gradlew runServer` | Starts a dedicated server with the mod loaded |
| `./gradlew runGameTestServer` | Starts a headless server, runs the game tests, and exits; a quick way to check that the mod loads cleanly |

On Windows, use `gradlew.bat` in place of `./gradlew`.

The mod version is set in one place, `mod_version` in `gradle.properties`. The build uses it for the jar's name and fills it into `mods.toml`, so change it there only.

### Project layout

| Path | Contents |
| --- | --- |
| `src/main/java/com/xincraft/xincraft/` | `XincraftMod` (entry point) and `Config` |
| `.../registry/` | Registration of blocks, items, block entities, entities, menus, data components, biome modifier types, tags, the tin trim material, and the creative tab |
| `.../block/` | Powered rail variants (padded, tin), the honey, bouncy, inspector, ender, and charging rails, the heat and steam turbines, the mixing tray, and the exotic flowers |
| `.../item/` | Ender rail item (attuning and glint), tin and bronze armor set bonuses, tin spyglass night vision, cart upgrade items, exotic pollen |
| `.../entity/` | Ender minecart, locomotive cart, jingle cart, cart upgrades (`CartUpgrades`), and the minecart physics behind tin, honey, and bouncy rails (`XincraftMinecartBehavior`) |
| `.../world/` | The biome modifier that adds tin ore, unless turned off in the config, and the reinforced trim template's mineshaft loot |
| `.../inventory/` | Locomotive cart and turbine menus (slots, burn progress, stored energy, and water) |
| `.../client/` | Client-only code: padded rail sound muffling, ender rail highlight, minecart renderers (with upgrades), locomotive cart screen (with energy bar), turbine screen, jingle cart music, tin spyglass tint |
| `src/main/resources/assets/xincraft/` | Block states, models, textures, and translations |
| `src/main/resources/data/` | Recipes, loot tables, tags, and tin ore generation (`xincraft/worldgen/`, `xincraft/forge/biome_modifier/`) |
