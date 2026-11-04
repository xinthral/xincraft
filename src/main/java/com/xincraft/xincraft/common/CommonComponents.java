package com.xincraft.xincraft.common;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

public final class CommonComponents {
    /**
     * @brief The sword a combat golem item's golem holds.
     *
     * @see golems.BronzeGolemItem
     */
    public static final RegistryObject<DataComponentType<ItemStack>> GOLEM_SWORD = Registers.COMPONENTS.register("golem_sword",
        () -> DataComponentType.<ItemStack>builder().persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC).build());

    /**
     * @brief The mobs inscribed on a mob filter.
     *
     * @see MobFilterItem
     */
    public static final RegistryObject<DataComponentType<MobFilterContents>> MOB_FILTER = Registers.COMPONENTS.register("mob_filter",
        () -> DataComponentType.<MobFilterContents>builder().persistent(MobFilterContents.CODEC).networkSynchronized(MobFilterContents.STREAM_CODEC).build());

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private CommonComponents() {}
}
