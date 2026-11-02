package com.xincraft.xincraft.flowers;

import java.util.Set;
import com.xincraft.xincraft.block.AuraFlowerBlockEntity;
import com.xincraft.xincraft.block.MixingTrayBlockEntity;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class FlowerBlockEntities {
    public static final RegistryObject<BlockEntityType<MixingTrayBlockEntity>> MIXING_TRAY = Registers.BLOCK_ENTITIES.register("mixing_tray",
        () -> new BlockEntityType<>(MixingTrayBlockEntity::new, Set.of(FlowerBlocks.MIXING_TRAY.get())));

    // The aura flowers share one block entity class, with a type each (block.AuraFlowerBlock)
    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> MENDING_LILAC = Registers.BLOCK_ENTITIES.register("mending_lilac",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(FlowerBlocks.MENDING_LILAC.get())));

    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> MOONLIT_DANDELION = Registers.BLOCK_ENTITIES.register("moonlit_dandelion",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(FlowerBlocks.MOONLIT_DANDELION.get())));

    public static final RegistryObject<BlockEntityType<AuraFlowerBlockEntity>> SHIMMER_LILY = Registers.BLOCK_ENTITIES.register("shimmer_lily",
        () -> new BlockEntityType<>(AuraFlowerBlockEntity::new, Set.of(FlowerBlocks.SHIMMER_LILY.get())));

    // Loading this class is what registers everything above
    static void init() {}

    private FlowerBlockEntities() {}
}
