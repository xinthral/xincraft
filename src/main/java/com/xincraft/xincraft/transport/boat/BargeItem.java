package com.xincraft.xincraft.transport.boat;

import com.xincraft.xincraft.transport.TransportEntityTypes;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;

/**
 * @brief Places a barge, filled with whatever the item holds.
 *
 * Like a shulker box, it can't be put inside a shulker box, a bundle, a barge or a freight cart.
 *
 * @see Barge
 */
public class BargeItem extends BoatItem {
    public BargeItem(Item.Properties properties) {
        super(TransportEntityTypes.BARGE.get(), properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
