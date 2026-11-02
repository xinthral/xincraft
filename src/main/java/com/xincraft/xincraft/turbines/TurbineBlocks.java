package com.xincraft.xincraft.turbines;

import com.xincraft.xincraft.block.TurbineBlock;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

public final class TurbineBlocks {
    // Generators that burn fuel into Forge Energy for charging rails (block.TurbineBlockEntity): the
    // tin heat turbine, and the bronze steam turbine, which also uses water
    public static final RegistryObject<Block> HEAT_TURBINE = Registers.registerBlock(
        "heat_turbine",
        TurbineBlock::heat,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(TurbineBlock.LIT) ? 13 : 0),
        true
    );

    public static final RegistryObject<Block> STEAM_TURBINE = Registers.registerBlock(
        "steam_turbine",
        TurbineBlock::steam,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(3.5F)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(TurbineBlock.LIT) ? 13 : 0),
        true
    );

    // Loading this class is what registers everything above
    static void init() {}

    private TurbineBlocks() {}
}
