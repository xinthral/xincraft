package com.xincraft.xincraft.item;

import com.xincraft.xincraft.registry.ModEntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;

// Places a storage cart (entity.StorageCart), filled with whatever the item holds. Like a shulker
// box, it can't be put inside a shulker box, a bundle or another storage cart.
public class StorageCartItem extends MinecartItem {
    public StorageCartItem(Item.Properties properties) {
        super(ModEntityTypes.STORAGE_CART.get(), properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
