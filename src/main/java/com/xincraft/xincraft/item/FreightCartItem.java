package com.xincraft.xincraft.item;

import com.xincraft.xincraft.transport.TransportEntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;

// Places a freight cart (entity.FreightCart), filled with whatever the item holds. Like a shulker
// box, it can't be put inside a shulker box, a bundle or another freight cart.
public class FreightCartItem extends MinecartItem {
    public FreightCartItem(Item.Properties properties) {
        super(TransportEntityTypes.FREIGHT_CART.get(), properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
