package com.xincraft.xincraft.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class CommonItems {
    // Fitted to a carriage or ferry, let only the mobs inscribed on them aboard (see common.MobFilterItem);
    // the fuzzy one also lets through their babies and kin
    public static final RegistryObject<Item> MOB_FILTER = Registers.registerItem("mob_filter",
        properties -> new MobFilterItem(2, false, properties), () -> new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> ADVANCED_MOB_FILTER = Registers.registerItem("advanced_mob_filter",
        properties -> new MobFilterItem(8, false, properties), () -> new Item.Properties().stacksTo(1));
    public static final RegistryObject<Item> FUZZY_MOB_FILTER = Registers.registerItem("fuzzy_mob_filter",
        properties -> new MobFilterItem(8, true, properties), () -> new Item.Properties().stacksTo(1));

    // Worn (or held), shows buoy paths; used on an RC motorboat on a path, starts and stops it
    public static final RegistryObject<Item> ROSE_TINTED_GLASSES = Registers.registerItem("rose_tinted_glasses", RoseTintedGlassesItem::new,
        () -> new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD));

    // Loading this class is what registers everything above
    static void init() {}

    private CommonItems() {}
}
