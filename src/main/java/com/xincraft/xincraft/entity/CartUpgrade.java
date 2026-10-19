package com.xincraft.xincraft.entity;

import java.util.function.Supplier;
import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

// The upgrades that can be fitted to the mod's minecarts (see CartUpgrades). Each is an item used
// on a cart, and is shown on the cart as a small model of its block.
public enum CartUpgrade {
    // Clears mobs and players off the track ahead, so the cart never picks them up or slows for them
    SHRIEKER("shrieker", ModItems.SHRIEKER_UPGRADE, Blocks.SCULK_SHRIEKER, false),
    // A jingle cart only plays while a player is within earshot
    SCULK_SENSOR("sculk_sensor", ModItems.SCULK_SENSOR_UPGRADE, Blocks.SCULK_SENSOR, true);

    private final String id;
    private final Supplier<Item> item;
    private final Block block;
    private final boolean jingleCartOnly;

    CartUpgrade(String id, Supplier<Item> item, Block block, boolean jingleCartOnly) {
        this.id = id;
        this.item = item;
        this.block = block;
        this.jingleCartOnly = jingleCartOnly;
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

    public boolean jingleCartOnly() {
        return jingleCartOnly;
    }

    // This upgrade's flag in the synced set of installed upgrades
    int bit() {
        return 1 << ordinal();
    }
}
