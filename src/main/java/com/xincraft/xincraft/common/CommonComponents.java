package com.xincraft.xincraft.common;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

public final class CommonComponents {
    // The sword a combat golem item's golem holds (see item.BronzeGolemItem)
    public static final RegistryObject<DataComponentType<ItemStack>> GOLEM_SWORD = Registers.COMPONENTS.register("golem_sword",
        () -> DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());

    // The mobs inscribed on a mob filter (see common.MobFilterItem)
    public static final RegistryObject<DataComponentType<MobFilterContents>> MOB_FILTER = Registers.COMPONENTS.register("mob_filter",
        () -> DataComponentType.<MobFilterContents>builder().persistent(MobFilterContents.CODEC).networkSynchronized(MobFilterContents.STREAM_CODEC).build());

    // Loading this class is what registers everything above
    static void init() {}

    private CommonComponents() {}
}
