package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

// The upgrades that can be fitted to the mod's minecarts (see CartUpgrades). Each is an item used
// on a cart, and is shown on the cart as a small model of its block.
public enum CartUpgrade {
    // Clears mobs and players off the track ahead, so the cart never picks them up or slows for them
    SHRIEKER("shrieker", ModItems.SHRIEKER_UPGRADE, Blocks.SCULK_SHRIEKER, null),
    // A jingle cart only plays while a player is within earshot
    SCULK_SENSOR("sculk_sensor", ModItems.SCULK_SENSOR_UPGRADE, Blocks.SCULK_SENSOR, JingleCart.class),
    // A locomotive cart stores Forge Energy and runs on it before burning fuel
    ENERGY("energy", ModItems.ENERGY_UPGRADE, Blocks.REDSTONE_BLOCK, LocomotiveCart.class);

    private final String id;
    private final Supplier<Item> item;
    private final Block block;
    private final @Nullable Class<? extends UpgradableCart> fitsOnly;

    CartUpgrade(String id, Supplier<Item> item, Block block, @Nullable Class<? extends UpgradableCart> fitsOnly) {
        this.id = id;
        this.item = item;
        this.block = block;
        this.fitsOnly = fitsOnly;
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

    // The one kind of cart this upgrade fits, or null if it fits them all
    public @Nullable Class<? extends UpgradableCart> fitsOnly() {
        return fitsOnly;
    }

    // This upgrade's flag in the synced set of installed upgrades
    int bit() {
        return 1 << ordinal();
    }
}
