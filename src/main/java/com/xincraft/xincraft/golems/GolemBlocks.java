package com.xincraft.xincraft.golems;

import com.xincraft.xincraft.block.BronzeChestBlock;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

public final class GolemBlocks {
    // A copper chest with a fourth row, and a bronze golem's home (block.BronzeChestBlock)
    public static final RegistryObject<Block> BRONZE_CHEST = Registers.registerBlock(
        "bronze_chest",
        BronzeChestBlock::new,
        BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_CHEST.weathering().unaffected()),
        true
    );

    // Loading this class is what registers everything above
    static void init() {}

    private GolemBlocks() {}
}
