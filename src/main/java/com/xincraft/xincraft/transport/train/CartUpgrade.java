package com.xincraft.xincraft.transport.train;

import java.util.List;
import java.util.function.Supplier;
import com.xincraft.xincraft.transport.TransportItems;
import com.xincraft.xincraft.transport.cart.Carriage;
import com.xincraft.xincraft.transport.cart.EnderMinecart;
import com.xincraft.xincraft.transport.boat.Ferry;
import com.xincraft.xincraft.transport.cart.JingleCart;
import com.xincraft.xincraft.transport.cart.LocomotiveCart;
import com.xincraft.xincraft.transport.boat.Motorboat;
import com.xincraft.xincraft.transport.boat.PartyBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * @brief The upgrades that can be fitted to the mod's minecarts and motorboats.
 *
 * Each is an item used on the vehicle, and is shown on it as a small model of its block.
 *
 * @see CartUpgrades
 */
public enum CartUpgrade {
    /**
     * @brief Clears mobs and players out of the vehicle's path, so it never picks them up or slows
     *        for them.
     */
    SHRIEKER("shrieker", TransportItems.SHRIEKER_UPGRADE, Blocks.SCULK_SHRIEKER, List.of()),
    /** @brief A jingle cart or party boat only plays while a player is within earshot. */
    SCULK_SENSOR("sculk_sensor", TransportItems.SCULK_SENSOR_UPGRADE, Blocks.SCULK_SENSOR, List.of(JingleCart.class, PartyBoat.class)),
    /** @brief The engine runs on a Forge Energy battery, which its fuel charges. */
    ENERGY("energy", TransportItems.ENERGY_UPGRADE, Blocks.REDSTONE_BLOCK, List.of(LocomotiveCart.class, Motorboat.class)),
    /** @brief A motorboat breaks the ice in front of it. */
    ICE_BREAKER("ice_breaker", TransportItems.ICE_BREAKER_UPGRADE, Blocks.STONECUTTER, List.of(Motorboat.class)),
    /** @brief A motorboat goes faster, burning its fuel faster. */
    SPEED("speed", TransportItems.SPEED_UPGRADE, Blocks.BLAST_FURNACE, List.of(Motorboat.class)),
    /** @brief Riders are invisible, but for the odd pink glint (the rideable vehicles only). */
    SHIMMER("shimmer", TransportItems.SHIMMER_UPGRADE, Blocks.TINTED_GLASS,
        List.of(EnderMinecart.class, LocomotiveCart.class, Carriage.class, Motorboat.class, Ferry.class)),
    /**
     * @brief A motorboat that comes across a buoy path follows it on its own.
     *
     * @see transport.boat.BuoyBlock
     */
    RC("rc", TransportItems.RC_UPGRADE, Blocks.LIGHTNING_ROD.weathering().unaffected(), List.of(Motorboat.class)),
    /**
     * @brief A mob riding a carriage or ferry is caged, its AI off, until it's let out.
     *
     * @see MobCell
     */
    CELL("cell", TransportItems.CELL_UPGRADE, Blocks.IRON_BARS, List.of(Carriage.class, Ferry.class)),
    /**
     * @brief Extends the RC upgrade's mast so the boat can follow the long legs between advanced
     *        buoys.
     *
     * Fits over the RC upgrade, and is drawn as its mast extended.
     *
     * @see transport.boat.AdvancedBuoyBlock
     */
    ANTENNA("antenna", TransportItems.ADVANCED_ANTENNA_UPGRADE, Blocks.LIGHTNING_ROD.weathering().unaffected(), List.of(Motorboat.class)),
    /**
     * @brief Backs the cell's bars with obsidian, so it holds any mob at all but the ender dragon,
     *        hauling aboard even those that won't ride.
     *
     * Fits over the cell, and is drawn as part of its cage.
     *
     * @see MobCell
     */
    REINFORCED_CAGE("reinforced_cage", TransportItems.REINFORCED_CAGE_UPGRADE, Blocks.OBSIDIAN, List.of(Carriage.class, Ferry.class));

    private final String id;
    private final Supplier<Item> item;
    private final Block block;
    private final List<Class<? extends UpgradableCart>> fits;

    CartUpgrade(String id, Supplier<Item> item, Block block, List<Class<? extends UpgradableCart>> fits) {
        this.id = id;
        this.item = item;
        this.block = block;
        this.fits = fits;
    }

    /**
     * @brief The upgrade's saved name, which never changes, unlike the order upgrades are declared
     *        in.
     *
     * @return The saved name.
     */
    public String id() {
        return id;
    }

    public Item item() {
        return item.get();
    }

    public Block block() {
        return block;
    }

    /**
     * @brief The upgrade that has to be fitted before this one, if any.
     *
     * @return The required upgrade, or null if there's none.
     */
    public @Nullable CartUpgrade requires() {
        return switch (this) {
            case ANTENNA -> RC;
            case REINFORCED_CAGE -> CELL;
            default -> null;
        };
    }

    /**
     * @brief The kinds of vehicle this upgrade fits; empty if it fits them all.
     *
     * @return The vehicle classes, or an empty list if it fits them all.
     */
    public List<Class<? extends UpgradableCart>> fits() {
        return fits;
    }

    /**
     * @brief This upgrade's flag in the synced set of installed upgrades.
     *
     * @return The flag.
     */
    int bit() {
        return 1 << ordinal();
    }
}
