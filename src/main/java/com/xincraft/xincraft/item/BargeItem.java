package com.xincraft.xincraft.item;

import com.xincraft.xincraft.registry.ModEntityTypes;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;

// Places a barge (entity.Barge), filled with whatever the item holds. Like a shulker box, it can't be
// put inside a shulker box, a bundle, a barge or a freight cart.
public class BargeItem extends BoatItem {
    public BargeItem(Item.Properties properties) {
        super(ModEntityTypes.BARGE.get(), properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
