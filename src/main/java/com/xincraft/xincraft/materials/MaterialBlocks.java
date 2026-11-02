package com.xincraft.xincraft.materials;

import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

public final class MaterialBlocks {
    public static final RegistryObject<Block> TIN_BLOCK = Registers.registerBlock(
        "tin_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(),
        true
    );

    // As hard as tin and iron blocks, with copper's sound
    public static final RegistryObject<Block> BRONZE_BLOCK = Registers.registerBlock(
        "bronze_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE)
            .strength(5.0F, 6.0F)
            .sound(SoundType.COPPER)
            .requiresCorrectToolForDrops(),
        true
    );

    // Tin ores generate in the Overworld (data/xincraft/worldgen, data/xincraft/forge/biome_modifier,
    // materials.TinOreBiomeModifier) unless Config.generateTinOre is off, and drop raw tin. Properties
    // match vanilla's iron ores.
    public static final RegistryObject<Block> TIN_ORE = Registers.registerBlock(
        "tin_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F),
        true
    );

    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = Registers.registerBlock(
        "deepslate_tin_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(4.5F, 3.0F)
            .sound(SoundType.DEEPSLATE),
        true
    );

    // Loading this class is what registers everything above
    static void init() {}

    private MaterialBlocks() {}
}
