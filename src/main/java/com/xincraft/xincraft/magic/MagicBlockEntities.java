package com.xincraft.xincraft.magic;

import java.util.Set;
import java.util.stream.Collectors;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class MagicBlockEntities {
    /**
     * @brief All the elemental flora share one type.
     *
     * @see FloraBlock
     */
    public static final RegistryObject<BlockEntityType<FloraBlockEntity>> FLORA = Registers.BLOCK_ENTITIES.register("flora",
        () -> new BlockEntityType<>(FloraBlockEntity::new,
            MagicBlocks.FLORA.values().stream().map(RegistryObject::get).collect(Collectors.toUnmodifiableSet())));

    public static final RegistryObject<BlockEntityType<ApothecaryAltarBlockEntity>> APOTHECARY_ALTAR = Registers.BLOCK_ENTITIES.register("apothecary_altar",
        () -> new BlockEntityType<>(ApothecaryAltarBlockEntity::new, Set.of(MagicBlocks.APOTHECARY_ALTAR.get())));

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MagicBlockEntities() {}
}
