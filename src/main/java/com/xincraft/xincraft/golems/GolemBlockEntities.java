package com.xincraft.xincraft.golems;

import java.util.Set;
import com.xincraft.xincraft.block.BronzeChestBlockEntity;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class GolemBlockEntities {
    public static final RegistryObject<BlockEntityType<BronzeChestBlockEntity>> BRONZE_CHEST = Registers.BLOCK_ENTITIES.register("bronze_chest",
        () -> new BlockEntityType<>(BronzeChestBlockEntity::new, Set.of(GolemBlocks.BRONZE_CHEST.get())));

    // Loading this class is what registers everything above
    static void init() {}

    private GolemBlockEntities() {}
}
