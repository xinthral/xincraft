# Xincraft

A Minecraft Forge mod that adds tin and bronze, plus a set of rails: fast tin rails, quiet padded rails, speed-limiting honey rails, launching bouncy rails, cart-sorting inspector rails, teleporting ender rails, and bronze charging rails, with heat and steam turbines to power them. It also adds minecarts: a self-driving locomotive cart and a music-playing jingle cart, motorboats, upgrades for both, exotic flowers that give off area effects, and the start of a nature magic system built on elemental flowers.

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
- [Freight Cart](#freight-cart)
- [Carriage and Linking Bar](#carriage-and-linking-bar)
- [Motorboats](#motorboats)
- [Boat Trains: Ferry and Barge](#boat-trains-ferry-and-barge)
- [Buoys](#buoys)
- [Cart Upgrades](#cart-upgrades)
- [Tin Spyglass](#tin-spyglass)
- [Exotic Flowers](#exotic-flowers)
- [Nature Magic](#nature-magic)
- [Xincraft Compendium](#xincraft-compendium)
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
| Minecart, empty Carriage | 1 |
| Carriage with a passenger | 2 |
| Minecart with Hopper | 3 |
| Minecart with Chest, Minecart with Furnace | 6 |
| Freight Cart | 1 (empty) to 15 (full), see below |
| Locomotive Cart | 9 |
| Ender Minecart | 12 |
| Minecart with TNT | 15 |

A carriage gives one more than its tag's signal while someone (a player or a mob) is riding it, so a station can tell a full carriage from an empty one; the signal rises as soon as they get in and drops within a second of them getting out. A **Freight Cart** gives a signal for **how full it is** instead: 1 when empty, 5 with a row's worth of full stacks, 10 with two rows, and 15 with three rows or anything over 85% full, stepping up evenly in between (anything in it at all gives at least 2). Half-full stacks count as half a stack, as a comparator counts them, and the signal follows changes within a second while the cart sits on the rail. Any other minecart (for example from another mod) gives 0, the same as an empty rail, until it's added to one of the signal tags below. If several carts are on the rail at once, it outputs the strongest signal. Like the detector rail, it strongly powers the block beneath it, powers adjacent powered rails, keeps its signal for up to a second after a cart leaves, and lets a comparator read how full a container cart on it is.

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

A rideable minecart with its own engine. While it has fuel burning, it drives itself forward, like a furnace minecart you can sit in. Only **players and villagers** can ride it; it won't pick up other mobs.

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
- **Handbrake:** hold **jump** (space) while riding to cut the engine and brake, sparks flying from the wheels, to a stop in under a second from full speed. It stays stopped for as long as you hold it, without burning new fuel. Let go and it carries on the way it was going, or, if it had stopped, press a movement key to set off again. The train it's towing is braked along with it.
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

## Freight Cart

A minecart carrying a **shulker box**, and working like one.

**Recipe** (shapeless):

> 1 Minecart Frame + 2 Shulker Shells + 1 Chest → 1 Freight Cart

- **Right-click** to open its 27 slots. It opens and closes with the shulker box sounds.
- **Breaking it keeps everything inside.** It drops as a single Freight Cart item holding its contents, which show in the item's tooltip, and placing it back on a rail brings them back. (In creative, breaking it spills the contents like a chest minecart.)
- Like a shulker box, it **can't hold shulker boxes or other freight carts**, and a freight cart can't go inside a shulker box or a bundle.
- **Hoppers load and unload it**, like a chest minecart, and a comparator reads how full it is from a detector or inspector rail. An [inspector rail](#inspector-rail) on its own also signals how full it is, from 1 when empty to 15 when full.
- Hitch it into a locomotive's train with a [Linking Bar](#carriage-and-linking-bar) to haul it along.

## Carriage and Linking Bar

A **Carriage** is a passenger car for your locomotive: a rideable minecart in maroon lacquer with bronze trim and windows, a carpeted floor and wood-panelled walls. On its own it rides like an ordinary minecart, except that it **won't pick up hostile mobs** (it still picks up animals and other mobs); fit a [Cell Upgrade](#cart-upgrades) to carry those. A [Mob Filter](#mob-filters) limits which mobs it takes. A **Linking Bar** hitches it, or any other Xincraft cart, into a train behind a [Locomotive Cart](#locomotive-cart), so the locomotive pulls it along.

**Recipes:**

> Carriage (shapeless): 1 Minecart Frame + 2 Bronze Ingots + 1 Wool (any colour) → 1 Carriage

| | | |
| --- | --- | --- |
| | | Bronze Ingot |
| | Iron Chain | |
| Bronze Ingot | | |

> Linking Bar (shaped): 2 Bronze Ingots and 1 Iron Chain on a diagonal → 1 Linking Bar

### Linking

- **Right-click one cart with the linking bar, then the other.** The bar glints between the two clicks. Click the same cart twice to cancel.
- **Carts link in a line**, each to one cart ahead and one behind, and **every train is led by a locomotive**. A locomotive tows **up to 3 carts**, counting every cart down the line.
- You can build a train from either end: link a cart to the locomotive, then the next cart to the last one in the train. Two short trains can be joined the same way, as long as the result has a locomotive at the front and no more than 3 carts behind it.
- The cart you click first goes in front when that makes a train; otherwise the other one does. Carriages, jingle carts, freight carts and other locomotives can all go in a train; a locomotive further back only counts as a cart. The ender minecart and vanilla minecarts can't be linked (the ender minecart because a train can't teleport through an ender rail together).
- **Boats link the same way**, behind a [motorboat](#boat-trains-ferry-and-barge) instead of a locomotive. A cart can't be linked to a boat.
- The carts must be within about 3 blocks of each other. The link draws them to just under 2 blocks apart, centre to centre.
- The same bar also links [buoys](#buoys).
- **Unhitching:** **shift + right-click** a cart with a linking bar to unhitch it from the cart ahead of it, taking any carts behind it along, and get the bar back. On the locomotive, it unhitches the cart behind. Breaking a cart unhitches the carts either side of it and drops a bar for each link, and so does the link snapping if the carts are pulled more than 5 blocks apart (for example by an ender rail teleporting one of them).

### Towing

- The locomotive drives as usual and pulls the train along behind it, each cart following the one ahead around curves, up and down slopes, and to a stop at an unpowered powered rail. If the locomotive runs backward, it pushes the train ahead of it instead.
- The two carts are joined by a **bronze drawbar** about two-thirds of a block long, with a **pivot pin** at each end. It swings with the carts through corners, and the linked carts never collide with or bump each other, so they don't jam on tight turns or slopes.
- Links are saved with the world.

> Like the other powered features, towing only works with the default minecart physics.

## Motorboats

Boats with a propeller and an engine that cruise along under their own power.

| Item | Recipe |
| --- | --- |
| Propeller | Bronze Ingot surrounded by 4 Tin Ingots (one on each side) |
| Motorboat | Any boat + 1 Propeller (shapeless) |

Every kind of boat works, and the motorboat keeps its wood: an oak boat makes an **Oak Motorboat**, a cherry boat a **Cherry Motorboat**, and so on, and a bamboo raft makes a **Bamboo Motor Raft**. Chest boats can't be made into motorboats.

### Driving

- **Right-click** to get in, as with any boat. It seats two.
- **Press forward** to start cruising. The motorboat keeps going under power at about **twice paddling speed** while you steer left and right, so you don't have to hold forward. Holding forward as well adds your paddling to the engine.
- **Press back** to stop the engine. It also cuts out when it runs out of fuel, when it runs into something and is stuck for a second, or when you get out.
- **The engine only pushes in water.** On land or ice it's an ordinary boat.
- While cruising, the propeller spins and churns up spray behind the boat, and the engine smokes while fuel is burning.

### Fuel

Fuel goes in the motorboat's **fuel slot**, and there's a **storage slot** for carrying anything. Open them with **Shift + right-click**, or press your **inventory key** while riding. It's the same screen as the locomotive cart's.

- It takes anything that burns in a furnace, and each item burns **8 times as long** as it would in a furnace (configurable). A lava bucket leaves its empty bucket behind.
- A new fuel item is only lit while the boat is cruising. An item that's already burning keeps burning down while you drift, like in a furnace.
- Breaking the motorboat drops it as an item, along with its fuel, storage and upgrades.

> **Upgrades** fit motorboats too: the shrieker, energy, ice breaker, speed and shimmer upgrades. See [Cart Upgrades](#cart-upgrades).

### Party Boat

A bronze-lined motorboat with a jukebox in its back seat.

| Item | Recipe |
| --- | --- |
| Bronze Hull Lining | 5 Bronze Ingots in a U shape (`B B` over `BBB`) |
| Party Boat | Any motorboat or motor raft + 1 Bronze Hull Lining + 1 Jukebox (shapeless) |

- It drives, fuels and takes upgrades like any motorboat, and always looks like an oak boat. The jukebox takes the second seat, so it carries one rider.
- **Right-click with a music disc** to put it in, swapping out any disc already there. **Shift + right-click with an empty hand** takes the disc out (with no disc in, that opens the fuel and storage slots as usual).
- Like the [Jingle Cart](#jingle-cart), it plays the disc **while it's moving**, the music following the boat, and starts the song over each time it sets off again. With the **Sculk Sensor Upgrade** it only plays while a player is within 32 blocks.
- Breaking it drops the disc along with the boat, its fuel, storage and upgrades.

## Boat Trains: Ferry and Barge

A motorboat can tow a train of boats the way a locomotive tows carts, hitched with the same [Linking Bar](#carriage-and-linking-bar) and by the same rules: each boat links to one ahead and one behind, every train is led by a **motorboat** (party boats included), and a motorboat tows **up to 3 boats**.

| Item | Recipe |
| --- | --- |
| Ferry | Any boat or raft + 2 Bronze Ingots + 1 Wool (any colour) (shapeless) |
| Barge | Any boat or raft + 2 Shulker Shells + 1 Chest (shapeless) |

- **Ferry:** the carriage's boat, a bamboo raft with maroon trim that seats two. Its passengers ride but **can't paddle or steer**, so it goes where it's towed and otherwise drifts. Like the carriage, it won't pick up hostile mobs unless it has a **Cell Upgrade**.
- **Barge:** the freight cart's boat, an oak boat carrying a shulker box, with the same **27 slots** that **stay inside when it's broken**. Right-click to open it. Nobody rides it, and it can't hold shulker boxes, freight carts or other barges.
- Both are listed with vanilla's boats, as are the motorboats and party boat: search **`#boats`** in the creative inventory. Likewise the Xincraft carts are listed with vanilla's minecarts in the `c:minecarts` tag: search **`#minecarts`**.
- **Towing:** each towed boat follows the one ahead and turns to face it. A motorboat hitched behind another is towed like any other boat: nobody aboard can steer it, and its engine and RC upgrade stay off until it's unhitched.
- A motorboat leading a train drives and follows [buoy](#buoys) courses as usual, pulling its train behind it.
- The boats must be within about 4 blocks of each other to link, and the link holds them about 2½ blocks apart, centre to centre. It snaps, dropping the bar, if they're pulled more than about 5½ blocks apart, for example if a towed boat runs aground.

## Buoys

Marker buoys linked into courses that motorboats with the **RC Upgrade** follow by themselves, for canals, harbour runs or a lazy-river ride.

| Item | Recipe |
| --- | --- |
| Buoy | 1 Iron Ingot + 1 Red Dye + 1 White Dye + 1 Torch (shapeless) → 2 |
| Advanced Buoy | 1 Buoy + 1 Bronze Ingot + 1 Glowstone Dust + 1 Blue Dye (shapeless) |
| Linking Bar | see [Carriage and Linking Bar](#carriage-and-linking-bar) |
| RC Upgrade, Advanced Antenna Upgrade | see [Cart Upgrades](#cart-upgrades) |
| Rose-Tinted Glasses | 2 Tin Ingots over 2 Pink Stained Glass Panes with a Rose Bush between them (`T T` / `PRP`) |

- **Placing:** right-click **still water** with a buoy, the way you place a lily pad. It's two blocks tall: a red-and-white float in the water and a pole with a small lamp above it, which glows. Boats, players and mobs pass straight through it. Breaking either half breaks the whole buoy.
- **Linking:** right-click one buoy with a **Linking Bar**, then the next. The bar glints in between, and one bar is used up per link. Linked buoys can be up to **32 blocks** apart, and each buoy takes **two links**, so a course is a line or a loop. **Shift + right-click** a buoy with a linking bar to unlink it and get its bars back; breaking a buoy drops its bars.
- **Advanced buoys:** blue and white, with a brighter lamp. Two advanced buoys can be linked up to **64 blocks** apart; linked to an ordinary buoy, an advanced buoy only reaches 32. A boat needs the **Advanced Antenna Upgrade** to follow a leg longer than 32 blocks. Without it, a long leg is the end of the course: the boat stops at the buoy before it and won't join the course along it.
- **Following:** a **cruising motorboat with the RC Upgrade** (party boats included) that comes within 2 blocks of a link starts following the course, steering from buoy to buoy. It takes gentle bends at full speed and **slows down before sharp turns**, braking about 2 blocks out (further when it's going faster). It keeps cruising **with nobody aboard** until the course ends, the fuel runs out, or it **runs aground**, when it just stops. A rider can still press back to stop. Boats without the upgrade ignore buoys.
- **Switching a buoy off:** right-click a buoy (with anything but a linking bar) to switch its lamp off, and again to switch it back on. A boat following the course toward a buoy that's off **cuts its engine within 6 blocks, eases to a stop about 2 blocks short of it, and waits there**, propeller still and burning no fuel. Switch the buoy back on and it sets off again, carrying on along the course, so an unlit buoy works like a stop signal at a pier.
- Links between buoys go in a straight line, so keep them over open water.

### Rose-Tinted Glasses

- **Wear them** (they go in the helmet slot) **or hold them** to see the links between buoys within 48 blocks, traced over the water. Worn, they also give your view a faint rose tint. A course is traced in **yellow**, like glowstone, when it makes a **complete loop** (every buoy linked to two others), and in **red**, like powered redstone, when it doesn't: it has an end.
- **Look at an elemental flora** through them to read the essence it holds out of its capacity, or **at an Apothecary Altar** to see the flora within its reach, element by element: how many there are, and how much essence they hold out of how much they can.
- **See who's in a cell:** a mob carried in a [Cell Upgrade](#cart-upgrades) shows as its spawn egg, and through the glasses the egg is labelled with the mob's name, as a name tag would show it (its custom name, if it has one), from up to 64 blocks away.
- **Right-click a motorboat with the RC Upgrade** with the glasses in hand when it's on a course with nobody steering to **start** it cruising, and again to **stop** it. A motorboat left on a loop will lap it until its fuel runs out.

## Cart Upgrades

Upgrades fit onto the Xincraft minecarts (ender minecart, locomotive cart, jingle cart, freight cart, and carriage) and boats (motorboats, ferry, and barge), and show as small blocks on the vehicle.

> [!IMPORTANT]
> **Removing upgrades: use shears.**
> Hold **shears** and **right-click the cart**. Each click snips off **one** upgrade, the **most recently fitted** first, and puts it back in your inventory (a message above the hotbar says which). With two fitted, click once for the newer one and again for the other. An empty hand or shift + right-click won't remove them.

- **Fit an upgrade** by right-clicking the cart with it. Each upgrade can be fitted once per cart, and only on the carts it fits (see the table); otherwise a message says why.
- Breaking the cart drops its upgrades.

| Upgrade | Recipe | Fits | Effect |
| --- | --- | --- | --- |
| Shrieker Upgrade | Sculk Shrieker surrounded by 4 Tin Ingots (one on each side) | Any Xincraft cart, motorboats | Clears the way ahead: mobs and players in the vehicle's path are shoved off to the side with a shriek before it reaches them. It never picks them up and is never slowed by them (it still bumps into other minecarts and boats). |
| Sculk Sensor Upgrade | Sculk Sensor surrounded by 4 Tin Ingots (one on each side) | Jingle Cart, Party Boat | The vehicle only plays while a player is within 32 blocks. |
| Energy Upgrade | Block of Redstone surrounded by 4 Tin Ingots (one on each side) | Locomotive Cart, motorboats | The vehicle stores Forge Energy (FE) in a battery that its fuel charges, and runs on that. See below. |
| Ice Breaker Upgrade | Iron Pickaxe surrounded by 4 Tin Ingots (one on each side) | Motorboats | Breaks the ice in front of the boat, turning it back into water, so it can cut through frozen rivers and lakes. Only works while the boat is in water, so it leaves ice roads alone. |
| Speed Upgrade | Blaze Rod surrounded by 4 Tin Ingots (one on each side) | Motorboats | Cruises 50% faster, burning fuel (or energy) twice as fast. |
| RC Upgrade | Lightning Rod surrounded by 4 Tin Ingots (one on each side) | Motorboats | Follows [buoy](#buoys) courses on its own, even with nobody aboard, slowing for sharp turns. |
| Advanced Antenna Upgrade | Lightning Rod surrounded by 4 Bronze Ingots (one on each side) | Motorboats with the RC Upgrade | Extends the RC Upgrade's mast like a car aerial, so the boat can follow the long legs, up to **64 blocks**, between [advanced buoys](#buoys). It has to go on after the RC Upgrade, and shears take it off first. |
| Shimmer Upgrade | Tinted Glass surrounded by 4 Tin Ingots (one on each side) | Ender minecart, locomotive cart, carriage, motorboats, ferry | Makes whoever's riding **invisible** (players and mobs alike), wearing off a few seconds after they get out. Every few seconds, at random, a faint pink glint flickers where the rider is, the only thing that gives them away. The vehicle itself stays visible. |
| Cell Upgrade | Iron Bars surrounded by 4 Tin Ingots (one on each side) | Carriage, ferry | Puts a low **cage of iron bars** around the carriage (or ferry), with the mob inside shown as its **spinning spawn egg** (a mob with no spawn egg shows as itself, standing out of the cage; [rose-tinted glasses](#rose-tinted-glasses) label the egg with the mob's name), and lets it pick up hostile mobs, which it otherwise won't. A mob it picks up (by rolling or floating into it, as any minecart or boat does) has its AI switched off with a clang, so it can't attack, shoot, explode or move while it's inside. The bars **shade it from the sun**, so zombies and skeletons don't burn, and **hold endermen in**: they can't teleport out, however they're hurt. Nothing in the cell can hurt anyone, even mobs like slimes that damage you just by touching you. **Right-click** the vehicle to let the mob out; it stays harmless for **1.5 seconds** after leaving, giving you time to step back, then gets its AI back. The same goes however it gets out: an activator rail, the vehicle breaking, or the upgrade being snipped off. **Leads:** right-click the vehicle (or the mob) with a lead to tie it ready, and the mob comes out of the cage already on the lead, held by you. This works for any mob that can be leashed, so not hostile mobs. If you're more than 12 blocks away when it comes out, the lead drops instead. With the cell empty, you can still ride it. |
| Reinforced Cage Upgrade | Iron Bars surrounded by 4 Obsidian (one on each side) | Carriage or ferry with the Cell Upgrade | Rebuilds the cage as an **obsidian frame with a glass window** on each side, through which you can see the spinning egg, and it holds **any mob except the Ender Dragon**: wardens, withers, iron golems, giant slimes, phantoms and the rest. It hauls in whatever mob it touches while it's empty, even mobs that normally won't ride anything, but passes over mobs on a lead and ones you've just let out. It has to go on after the Cell Upgrade, and shears take it off first. Players can still ride normally. A mob with no spawn egg shows as itself, so a big one sticks out of the top. |

> The shrieker, like the other powered features, only works with the default minecart physics.

### Energy Upgrade

For modpacks with Forge Energy (FE) mods. A locomotive cart with the energy upgrade holds up to **100,000 FE** and uses **20 FE per tick** while driving, about 4 minutes of driving on a full charge.

- **Charging:** the cart draws up to 1,000 FE per tick from a [Charging Rail](#charging-rail) it's on, which makes a charging station. It also draws from an energy block (a battery, energy cell, generator and so on) **under the rail** it's on, the way a hopper under the rail empties a chest minecart, from other mods' charging rails, and from anything else that hands energy to entities. Energy can't be drawn back out of the cart.
- **Fuel charges the battery:** whenever the battery isn't full, the cart burns fuel from its fuel slot, moving or parked, and every tick of burning goes into the battery (20 FE by default, the same as a tick of driving uses). So fuel lasts just as long as it would without the upgrade. Once the battery is full it stops lighting new fuel, so a parked locomotive with fuel loaded tops itself up and then waits.
- **The battery drives the cart:** with the upgrade, the engine runs only on stored energy, throwing off sparks (and smoke while fuel is burning). While moving with fuel loaded, the fuel refills the battery as fast as driving drains it; once the fuel runs out, the battery keeps it going until it's empty.
- **Energy bar:** the cart's inventory screen shows the charge as a red bar left of the fuel slot. Hover over it for the exact amount.
- The stored energy stays with the cart if the upgrade is removed, but the cart can't use it or charge until the upgrade is fitted again. Breaking the cart loses it.
- The capacity and energy per tick can be changed in the config (`energyCapacity` and `energyPerTick` under `locomotiveCart`).

**On a motorboat** the energy upgrade works the same way. Fuel charges the battery whenever it isn't full, and the engine runs only on the battery: 100,000 FE, using 20 FE per tick while cruising (40 FE with the speed upgrade). Instead of a charging rail, it charges from energy blocks **right next to it**, such as a turbine on a dock. The battery's settings are under `motorboat` in the config.

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
| Wither Rose | **Shimmer Lily** | **Invisibility** for players within 8 blocks (a 16×16×16 area) |

- **Mending Lilac:** like a beacon, it refreshes the effect every 2 seconds, so it stays on while you're in range and wears off a few seconds after you leave. Several lilacs don't make it stronger. The range can be changed in the config (`flowers.mendingLilacRange`).
- **Moonlit Dandelion:** a pale silver bloom that sheds the odd glimmer of light. Like the lilac, it refreshes Night Vision every 2 seconds. Each dose lasts 15 seconds, so the screen doesn't flicker as it runs low, and it wears off about 15 seconds after you leave. The range can be changed in the config (`flowers.moonlitDandelionRange`).
- **Shimmer Lily:** a pale, shimmering pink bloom that gives off the odd pink glint, like the shimmer upgrade. Its Invisibility shows no swirling effect particles, so nothing gives you away, and wears off about 5 seconds after you leave. It grows wherever a wither rose can, soul sand and netherrack included, but it doesn't wither anything. The range can be changed in the config (`flowers.shimmerLilyRange`).
- **Firefly Petunia:** bright enough to stop mobs spawning around it, while still giving a garden a soft glow.
- Both are planted and broken like any flower (they break instantly and drop themselves), bees pollinate them, and they work in suspicious stew. They also craft into dye: a Mending Lilac makes Magenta Dye, a Firefly Petunia Yellow Dye, a Moonlit Dandelion Light Gray Dye and a Shimmer Lily Pink Dye.
- They can't go in flower pots yet.

### Glowing leaves

Right-click any **leaves** with Exotic Pollen to make them glow faintly, about as bright as glow lichen (light level 7). It uses one pollen per block, and works on every kind of vanilla leaves: oak, spruce, birch, jungle, acacia, cherry, dark oak, pale oak, mangrove, azalea and flowering azalea.

Glowing leaves look exactly like the leaves they came from and behave the same way. They decay when their tree is cut down, and drop the same saplings, sticks and apples. Broken with shears or Silk Touch, they drop ordinary leaves, so the glow stays where it was applied and doesn't come with you.

## Nature Magic

Flowers hold the primal elements: **Terra, Aqua, Ignis, Aer, Vita, Lux and Umbra**. You grind them into powders, then use a powder to turn a living flower into an **elemental flora** that stores that element's essence. The flora are the reservoirs for rituals coming in a later update.

### Mortar and Pestle

**Recipe:** a flint in the top-right corner over 3 stone (cobblestone, blackstone or cobbled deepslate) in a bowl shape (`  F` / `S S` / ` S `) → 1 Mortar and Pestle.

It isn't used up when you grind with it. It stays in the crafting grid and wears down by one each time, lasting 64 grinds.

### Elemental powders

Each powder is a shapeless recipe: the **mortar and pestle**, a **vine** and two flowers, making **2 powders**.

| Powder | Flowers |
| --- | --- |
| Ignis | Poppy + Torchflower |
| Aqua | Blue Orchid + Cornflower |
| Aer | Azure Bluet + White Tulip |
| Terra | Dandelion + Oxeye Daisy |
| Vita | Lily of the Valley + Pink Tulip |
| Lux | Sunflower + Orange Tulip |
| Umbra | Wither Rose or Closed Eyeblossom + Allium |

### Elemental flora

**Right-click any small flower with a powder** to turn it into that element's flora (Ignis powder makes a **Flora Ignis**, Aqua powder a **Flora Aquae**, and so on). It starts with 16 essence and uses up the powder. The mod's exotic flowers can't be changed this way.

- **Topping up:** right-click a flora with more of its own element's powder to add 16 essence, up to 64. Other powders do nothing.
- **Checking:** right-click a flora with an empty hand to see how much essence it holds.
- The fuller it is, the deeper its colour, from pale and washed-out when empty to the element's full colour, and the more it glows (up to light level 8) and sparkles.
- **Breaking a flora destroys it.** It drops nothing, and its essence is lost. Pistons and flowing water break it too.

### Apothecary Altar

**Recipe:** two elemental powders (any kind) in the top corners, stone bricks either side of a campfire, and 3 stone bricks along the bottom (`P P` / `SCS` / `SSS`) → 1 Apothecary Altar. Mine it with a pickaxe.

The altar burns plants into elemental essence, which flows into the flora around it. It works like a furnace: put a **plant** in the top slot and **fuel** (anything that burns in a furnace) in the bottom slot.

- Each plant takes 5 seconds to burn and gives **4 essence of each of its elements**. A stream of the element's colour flows from the altar to each flora that takes some, emptiest first.
- It reaches flora **up to 4 blocks away** in every direction.
- Look at the altar through **Rose-Tinted Glasses** to see how full the flora in its reach are.
- It only burns while a flora of one of the plant's elements is in range and has room, so plants and fuel aren't wasted. Fuel that's already lit burns out, as in a furnace.
- Only plants go in the plant slot. Which plant gives which element is set by the item tags `xincraft:essence/<element>`, so a data pack can change them:

| Element | Plants |
| --- | --- |
| Terra | Saplings, Dandelion, Oxeye Daisy, Dead Bush, Short Grass, Fern, Wheat Seeds |
| Aqua | Kelp, Seagrass, Lily Pad, Sea Pickle, Sugar Cane, Blue Orchid, Cornflower |
| Ignis | Poppy, Torchflower, Nether Wart, Crimson Fungus, Crimson Roots, Cactus |
| Aer | Vines, Leaves, Azure Bluet, White Tulip, Spore Blossom |
| Vita | Lily of the Valley, Pink Tulip, Sweet Berries, Moss Block, Pink Petals |
| Lux | Sunflower, Orange Tulip, Glow Berries, Glow Lichen |
| Umbra | Wither Rose, Closed Eyeblossom, Allium, Red and Brown Mushrooms, Warped Fungus, Warped Roots |

- **The screen** shows a row of seven pips, one per element. A pip is dark when no flora of its element is in range, dim when they're all full, and bright when one has room. It's outlined in white when the plant in the slot gives that element. Hover a pip to see which.
- **Hoppers** put plants in from above and fuel in from the sides, and take empty buckets out from below.
- Breaking the altar drops what's in it.

### Apothecary Crucible

**Recipe:** 3 elemental powders (any kind) along the top, a cauldron below the middle one (`PPP` / ` C `) → 1 Apothecary Crucible. Mine it with a pickaxe.

The crucible **sits on top of an Apothecary Altar** (it can't be placed anywhere else, and breaks off if the altar goes) and inscribes mobs onto [Mob Filters](#mob-filters). Right-click it to open it. Like a crafting table it holds nothing: whatever you leave in it comes back to you when you close it.

- **Slots:** an elemental powder in each top corner, a **catalyst** item in the middle, and a mob filter at the bottom. The output, on the right, is the same filter with the mob added; taking it uses up the powders, the catalyst and the original filter.
- **Which mob:** the two powders and the catalyst together name the mob (see the table below). The powders can go in either corner.
- **Babies:** Vita stands for youth, so no recipe uses it. For a mob that has babies, swap **either** of its powders for **Vita powder** and the same catalyst inscribes the **baby** instead: Aer + Terra + Feather is a Chicken, Aer + Vita + Feather a Baby Chicken. In a recipe with **Umbra**, though, Vita has to take the **Umbra's** place (life driving out death): Terra + Umbra + Rotten Flesh is a Zombie, and only Terra + Vita + Rotten Flesh a Baby Zombie.
- **Nature's balance:** the altar doesn't have to be lit, but the flora around it (within the altar's reach) have to hold essence. For **each powder** burnt, the crucible draws **1 essence of every element that isn't one of the powders**. Aer + Terra draws 2 Aqua, 2 Ignis, 2 Vita, 2 Lux and 2 Umbra; Terra + Terra draws 2 of each of the other six. A stream of each element's colour flows from the flora drawn on to the crucible.
- **The screen** shows a row of seven element pips: lit for the elements the powders will draw on, dim for the rest, and outlined in **red** when the flora around the altar are short of one. The **filter slot glows red** when the filter is full or already lists that mob, and then there's no output. Hover the empty output to see what's missing.

The recipes are data-driven (`data/xincraft/recipe/crucible/<mob>.json`, type `xincraft:crucible`), so a data pack can change or add them. Undead kin with no item of their own share their living (or plainer) counterpart's catalyst, with Umbra among the powders:

| Mob | Powders | Catalyst | Baby |
| --- | --- | --- | --- |
| Chicken | Aer + Terra | Feather | ✓ |
| Cow | Terra + Terra | Raw Beef | ✓ |
| Mooshroom | Terra + Umbra | Red Mushroom | ✓ |
| Pig | Terra + Terra | Raw Porkchop | ✓ |
| Sheep | Aer + Terra | Any Wool | ✓ |
| Rabbit | Terra + Terra | Rabbit's Foot | ✓ |
| Goat | Aer + Terra | Goat Horn | ✓ |
| Horse | Aer + Terra | Golden Carrot | ✓ |
| Donkey | Terra + Terra | Chest | ✓ |
| Zombie Horse | Terra + Umbra | Golden Carrot | |
| Skeleton Horse | Aer + Umbra | Golden Carrot | |
| Mule | Terra + Umbra | Hay Bale | ✓ |
| Llama | Aer + Terra | Any Carpet | ✓ |
| Trader Llama | Lux + Terra | Lead | ✓ |
| Camel | Ignis + Terra | Cactus | ✓ |
| Camel Husk | Ignis + Umbra | Cactus | |
| Panda | Lux + Terra | Bamboo | ✓ |
| Polar Bear | Aqua + Terra | Packed Ice | ✓ |
| Fox | Terra + Umbra | Sweet Berries | ✓ |
| Wolf | Terra + Umbra | Bone | ✓ |
| Cat | Terra + Umbra | String | ✓ |
| Ocelot | Ignis + Terra | Cocoa Beans | ✓ |
| Armadillo | Ignis + Terra | Armadillo Scute | ✓ |
| Sniffer | Terra + Terra | Sniffer Egg | ✓ |
| Turtle | Aqua + Terra | Turtle Scute | ✓ |
| Frog | Aqua + Terra | Lily Pad |  |
| Tadpole | Aqua + Aqua | Bucket of Tadpole |  |
| Axolotl | Aqua + Lux | Bucket of Axolotl | ✓ |
| Bee | Aer + Lux | Honeycomb | ✓ |
| Parrot | Aer + Aer | Wheat Seeds |  |
| Bat | Aer + Umbra | Apple | |
| Allay | Aer + Lux | Amethyst Shard |  |
| Happy Ghast | Aer + Lux | Dried Ghast | ✓ |
| Strider | Ignis + Ignis | Warped Fungus | ✓ |
| Hoglin | Ignis + Terra | Crimson Fungus | ✓ |
| Zoglin | Umbra + Umbra | Crimson Fungus | ✓ |
| Piglin | Ignis + Lux | Gold Ingot | ✓ |
| Piglin Brute | Ignis + Umbra | Golden Axe |  |
| Zombified Piglin | Ignis + Umbra | Gold Nugget | ✓ |
| Villager | Lux + Terra | Emerald | ✓ |
| Wandering Trader | Aer + Lux | Milk Bucket |  |
| Iron Golem | Lux + Terra | Iron Ingot |  |
| Snow Golem | Aer + Aqua | Snowball |  |
| Copper Golem | Lux + Terra | Block of Copper |  |
| Cod / Salmon / Pufferfish / Tropical Fish | Aqua + Aqua | Raw Cod / Raw Salmon / Pufferfish / Tropical Fish |  |
| Squid | Aqua + Umbra | Ink Sac | ✓ |
| Glow Squid | Aqua + Lux | Glow Ink Sac | ✓ |
| Dolphin | Aer + Aqua | Heart of the Sea | ✓ |
| Guardian | Aqua + Umbra | Prismarine Shard |  |
| Elder Guardian | Aqua + Umbra | Wet Sponge |  |
| Nautilus | Aqua + Aqua | Nautilus Shell | ✓ |
| Zombie Nautilus | Aqua + Umbra | Nautilus Shell | |
| Sulfur Cube | Ignis + Terra | Bucket of Sulfur Cube |  |
| Zombie | Terra + Umbra | Rotten Flesh | ✓ |
| Husk | Ignis + Umbra | Rotten Flesh | ✓ |
| Drowned | Aqua + Umbra | Copper Ingot | ✓ |
| Zombie Villager | Lux + Umbra | Golden Apple | ✓ |
| Skeleton | Terra + Umbra | Arrow |  |
| Stray | Aer + Umbra | Arrow | |
| Parched | Ignis + Umbra | Arrow | |
| Bogged | Aqua + Umbra | Brown Mushroom |  |
| Wither Skeleton | Ignis + Umbra | Coal |  |
| Wither | Umbra + Umbra | Nether Star |  |
| Creeper | Ignis + Umbra | Gunpowder |  |
| Spider | Terra + Umbra | Spider Eye |  |
| Cave Spider | Terra + Umbra | Cobweb |  |
| Witch | Lux + Umbra | Glowstone Dust |  |
| Enderman | Aer + Umbra | Ender Pearl |  |
| Endermite | Terra + Umbra | Chorus Fruit | |
| Shulker | Lux + Umbra | Shulker Shell |  |
| Silverfish | Terra + Umbra | Stone Bricks | |
| Slime | Aqua + Terra | Slimeball |  |
| Magma Cube | Ignis + Ignis | Magma Cream |  |
| Blaze | Aer + Ignis | Blaze Rod |  |
| Breeze | Aer + Aer | Breeze Rod |  |
| Ghast | Aer + Ignis | Ghast Tear |  |
| Phantom | Aer + Umbra | Phantom Membrane |  |
| Creaking | Terra + Umbra | Resin Clump |  |
| Pillager | Terra + Umbra | Crossbow |  |
| Vindicator | Terra + Umbra | Iron Axe |  |
| Vex | Aer + Umbra | Iron Axe | |
| Evoker | Lux + Umbra | Totem of Undying |  |
| Ravager | Terra + Umbra | Saddle |  |
| Warden | Umbra + Umbra | Echo Shard |  |

### Mob Filters

A mob filter, fitted to a [Carriage](#carriage-and-linking-bar) or [Ferry](#boat-trains-ferry-and-barge), lets **only the mobs inscribed on it** aboard; anything else it bumps into is left behind. Players can always ride. It works with or without the Cell Upgrade (hostile mobs still need the cell), and the reinforced cage only hauls in mobs on the filter.

| Filter | Recipe | Holds | Matches |
| --- | --- | --- | --- |
| Mob Filter | Iron Bars surrounded by 4 Paper (one on each side) | 2 mobs | Exactly: a Chicken isn't a Baby Chicken, and a Drowned isn't a Zombie |
| Advanced Mob Filter | A Mob Filter surrounded by 4 Amethyst Shards (one on each side) | 8 mobs | Exactly, as the Mob Filter |
| Fuzzy Mob Filter | An Advanced Mob Filter surrounded by 8 Wool | 8 mobs | The mob **at any age**, and **its kin** |

- **Inscribing:** in an [Apothecary Crucible](#apothecary-crucible). The tooltip lists the mobs on a filter and how many more it takes. Crafting a filter into a bigger one gives a blank filter.
- **Fitting:** right-click the carriage or ferry with the filter. Using another filter swaps them, handing the old one back. **Shears** snip the filter off before any upgrade, and breaking the vehicle drops it.
- **Wiping:** right-click a water cauldron with an inscribed filter to wash it blank, using a level of water.
- **Kin** (for the fuzzy filter), set by the entity type tags `xincraft:mob_family/<name>`: zombies (Zombie, Husk, Drowned, Zombie Villager); skeletons (Skeleton, Stray, Bogged, Parched, Wither Skeleton); horses (Horse, Donkey, Mule, Skeleton Horse, Zombie Horse); llamas (Llama, Trader Llama); camels (Camel, Camel Husk); cows (Cow, Mooshroom); piglins (Piglin, Piglin Brute, Zombified Piglin); hoglins (Hoglin, Zoglin); nautiluses (Nautilus, Zombie Nautilus); spiders (Spider, Cave Spider); slimes (Slime, Magma Cube, Sulfur Cube); squids (Squid, Glow Squid); guardians (Guardian, Elder Guardian); ghasts (Ghast, Happy Ghast); fish (Cod, Salmon, Pufferfish, Tropical Fish); illagers (Pillager, Vindicator, Evoker); golems (Iron, Snow and Copper Golem); frogs (Frog, Tadpole); cats (Cat, Ocelot).

## Xincraft Compendium

An in-game guide book with an entry for every block, item, vehicle and mechanic in the mod. Craft a Book with any elemental powder, or a Book with a Minecart Frame (both shapeless), and right-click to read it.

- **Categories:** Magic (Natural, Metallurgic) and Transport (Rails, Boats). Each category opens with an overview of how its mechanics fit together.
- **Recipes** are drawn as crafting, smelting and smithing grids, sent by the server when you join and again after `/reload`, so recipes changed by a data pack show up as they really are. The Apothecary Crucible entry lists every crucible recipe.
- **Navigating:** click links and items to jump to their entries; the arrow buttons, arrow keys or mouse wheel turn pages, and Backspace goes back. The contents page has a search box that matches entry titles and the names of their items.
- **Bookmarks:** the ribbon button marks the open entry; up to 8 bookmarks hang off the book's right edge. Bookmarks and the last page read are kept in `config/xincraft-compendium.json`.
- **Look-up key** (G by default, under Miscellaneous in Controls): with a compendium anywhere in your inventory, opens the entry for the item under the mouse in any inventory screen, or for the block or vehicle you're looking at.

The book's text lives in resource packs at `assets/xincraft/compendium/<language>/` (`categories/` and `entries/`), falling back to `en_us`, so it can be translated or rewritten without touching code.

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

[motorboat]
	fuelMultiplier = 8.0
	energyCapacity = 100000
	energyPerTick = 20

[flowers]
	mendingLilacRange = 8
	moonlitDandelionRange = 8
	shimmerLilyRange = 8

[flora]
	capacity = 64
	initialEssence = 16
	essencePerPowder = 16

[altar]
	range = 4
	burnTicks = 100
	essencePerElement = 4

[crucible]
	balanceEssence = 1

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
| `motorboat.fuelMultiplier` | `8.0` | 1.0 – 64.0 | How many times longer fuel burns in a motorboat than in a furnace |
| `motorboat.energyCapacity` | `100000` | 1,000 – 100,000,000 | How much Forge Energy (FE) a motorboat with the energy upgrade can store |
| `motorboat.energyPerTick` | `20` | 1 – 100,000 | How much Forge Energy (FE) a motorboat with the energy upgrade uses each tick while cruising (twice this with the speed upgrade) |
| `flowers.mendingLilacRange` | `8` | 1 – 32 | How far, in blocks, a mending lilac's regeneration reaches in each direction (8 = a 16-block-wide area) |
| `flowers.moonlitDandelionRange` | `8` | 1 – 32 | How far, in blocks, a moonlit dandelion's night vision reaches in each direction (8 = a 16-block-wide area) |
| `flowers.shimmerLilyRange` | `8` | 1 – 32 | How far, in blocks, a shimmer lily's invisibility reaches in each direction (8 = a 16-block-wide area) |
| `flora.capacity` | `64` | 1 – 100,000 | How much essence an elemental flora can hold |
| `flora.initialEssence` | `16` | 0 – 100,000 | Essence a flower holds when a powder first turns it into a flora |
| `flora.essencePerPowder` | `16` | 1 – 100,000 | Essence each further powder of the same element adds to a flora |
| `altar.range` | `4` | 1 – 8 | How far, in blocks, the apothecary altar reaches in each direction for flora to fill (4 = a 9-block-wide cube) |
| `altar.burnTicks` | `100` | 1 – 6,000 | Ticks the altar takes to burn one plant (20 = one second) |
| `altar.essencePerElement` | `4` | 1 – 100,000 | Essence each burnt plant gives for each of its elements |
| `crucible.balanceEssence` | `1` | 0 – 100,000 | Essence an apothecary crucible inscription draws from the flora around the altar, of every element not among its powders, for each powder burnt |
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
| `.../block/` | Powered rail variants (padded, tin), the honey, bouncy, inspector, ender, and charging rails, the heat and steam turbines, the mixing tray, the exotic flowers, and buoys |
| `.../item/` | Ender rail item (attuning and glint), tin and bronze armor set bonuses, tin spyglass night vision, cart upgrade items, exotic pollen, the linking bar, and the buoy item |
| `.../entity/` | Ender minecart, locomotive cart, jingle cart, carriage, freight cart, motorboats, upgrades for carts and boats (`CartUpgrades`), the ferry and barge, linking carts and boats into trains (`LinkedTransport`), the freight cart's and barge's shulker box storage (`ShulkerHold`), following buoy paths (`BuoyPath`), and the minecart physics behind tin, honey, and bouncy rails (`XincraftMinecartBehavior`) |
| `.../world/` | The biome modifier that adds tin ore, unless turned off in the config, and the reinforced trim template's mineshaft loot |
| `.../inventory/` | Locomotive cart and turbine menus (slots, burn progress, stored energy, and water) |
| `.../client/` | Client-only code: padded rail sound muffling, ender rail highlight, buoy path highlight, minecart and motorboat renderers (with upgrades and linking bars), locomotive cart screen (with energy bar), turbine screen, jingle cart music, tin spyglass tint, and the compendium (`compendium/`) with its look-up key |
| `.../network/` | The mod's network channel and the packet that sends recipes to the compendium |
| `src/main/resources/assets/xincraft/` | Block states, models, textures, and translations |
| `src/main/resources/data/` | Recipes, loot tables, tags, and tin ore generation (`xincraft/worldgen/`, `xincraft/forge/biome_modifier/`) |

## License

Xincraft is released under the [MIT License](LICENSE.txt): you can use, modify and redistribute it, including in modpacks, as long as the copyright notice comes along.
