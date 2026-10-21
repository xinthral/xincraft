package com.xincraft.xincraft.entity;

import java.util.List;
import java.util.function.Supplier;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// The upgrades that can be fitted to the mod's minecarts and motorboats (see CartUpgrades). Each is
// an item used on the vehicle, and is shown on it as a small model of its block.
public enum CartUpgrade {
    // Clears mobs and players out of the vehicle's path, so it never picks them up or slows for them
    SHRIEKER("shrieker", ModItems.SHRIEKER_UPGRADE, Blocks.SCULK_SHRIEKER, List.of()),
    // A jingle cart only plays while a player is within earshot
    SCULK_SENSOR("sculk_sensor", ModItems.SCULK_SENSOR_UPGRADE, Blocks.SCULK_SENSOR, List.of(JingleCart.class)),
    // The engine runs on a Forge Energy battery, which its fuel charges
    ENERGY("energy", ModItems.ENERGY_UPGRADE, Blocks.REDSTONE_BLOCK, List.of(LocomotiveCart.class, Motorboat.class)),
    // A motorboat breaks the ice in front of it
    ICE_BREAKER("ice_breaker", ModItems.ICE_BREAKER_UPGRADE, Blocks.STONECUTTER, List.of(Motorboat.class)),
    // A motorboat goes faster, burning its fuel faster
    SPEED("speed", ModItems.SPEED_UPGRADE, Blocks.BLAST_FURNACE, List.of(Motorboat.class));

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

    // Saved name; never changes, unlike the order upgrades are declared in
    public String id() {
        return id;
    }

    public Item item() {
        return item.get();
    }

    public Block block() {
        return block;
    }

    // The kinds of vehicle this upgrade fits; empty if it fits them all
    public List<Class<? extends UpgradableCart>> fits() {
        return fits;
    }

    // This upgrade's flag in the synced set of installed upgrades
    int bit() {
        return 1 << ordinal();
    }
}
