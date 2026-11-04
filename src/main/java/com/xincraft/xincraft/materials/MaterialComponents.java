package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraftforge.registries.RegistryObject;

public final class MaterialComponents {
    /**
     * @brief Marks a vanilla spyglass as a tin spyglass.
     *
     * @see TinSpyglass
     */
    public static final RegistryObject<DataComponentType<Unit>> TIN_LENS = Registers.COMPONENTS.register("tin_lens",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MaterialComponents() {}
}
