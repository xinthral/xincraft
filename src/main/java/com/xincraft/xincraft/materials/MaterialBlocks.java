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

    /** @brief As hard as tin and iron blocks, with copper's sound. */
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

    /**
     * @brief Tin ores, which generate in the Overworld unless Config.generateTinOre is off, and
     *        drop raw tin.
     *
     * Properties match vanilla's iron ores.
     *
     * @see TinOreBiomeModifier
     */
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

    /**
     * @brief Silver ores, which generate deeper and more rarely than tin unless
     *        Config.generateSilverOre is off, and drop raw silver.
     *
     * Properties match vanilla's gold ores.
     *
     * @see SilverOreBiomeModifier
     */
    public static final RegistryObject<Block> SILVER_ORE = Registers.registerBlock(
        "silver_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 3.0F),
        true
    );

    public static final RegistryObject<Block> DEEPSLATE_SILVER_ORE = Registers.registerBlock(
        "deepslate_silver_ore",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(4.5F, 3.0F)
            .sound(SoundType.DEEPSLATE),
        true
    );

    public static final RegistryObject<Block> SILVER_BLOCK = Registers.registerBlock(
        "silver_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(),
        true
    );

    /** @brief A pale gold, needing an iron pickaxe like a block of gold. */
    public static final RegistryObject<Block> ELECTRUM_BLOCK = Registers.registerBlock(
        "electrum_block",
        Block::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops(),
        true
    );

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private MaterialBlocks() {}
}
