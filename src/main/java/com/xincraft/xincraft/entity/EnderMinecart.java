package com.xincraft.xincraft.entity;

import com.xincraft.xincraft.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A rideable minecart on ender wheels. Unless the config allows other minecarts, it's the only
// cart that ender rails will teleport (see block.EnderRailBlock).
public class EnderMinecart extends Minecart {
    public EnderMinecart(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.ENDER_MINECART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ENDER_MINECART.get());
    }
}
