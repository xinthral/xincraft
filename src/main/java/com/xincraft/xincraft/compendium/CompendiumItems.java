package com.xincraft.xincraft.compendium;

import com.xincraft.xincraft.item.CompendiumItem;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class CompendiumItems {
    // The mod's guide book (item.CompendiumItem)
    public static final RegistryObject<Item> COMPENDIUM = Registers.registerItem("compendium", CompendiumItem::new,
        () -> new Item.Properties().stacksTo(1));

    // Loading this class is what registers everything above
    static void init() {}

    private CompendiumItems() {}
}
