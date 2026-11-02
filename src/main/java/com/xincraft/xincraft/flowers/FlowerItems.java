package com.xincraft.xincraft.flowers;

import com.xincraft.xincraft.item.ExoticPollenItem;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.RegistryObject;

public final class FlowerItems {
    // Mixed from three different flowers on the mixing tray; turns certain flowers into exotic ones
    public static final RegistryObject<Item> EXOTIC_POLLEN = Registers.registerItem("exotic_pollen", ExoticPollenItem::new,
        () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // Loading this class is what registers everything above
    static void init() {}

    private FlowerItems() {}
}
