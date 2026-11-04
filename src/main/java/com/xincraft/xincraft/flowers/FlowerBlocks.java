package com.xincraft.xincraft.flowers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.xincraft.xincraft.common.Registers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

public final class FlowerBlocks {
    /**
     * @brief The tray that exotic pollen is made on.
     *
     * @see MixingTrayBlock
     * @see ExoticPollenItem
     */
    public static final RegistryObject<Block> MIXING_TRAY = Registers.registerBlock(
        "mixing_tray",
        MixingTrayBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(0.5F)
            .sound(SoundType.WOOD),
        true
    );

    /**
     * @brief Exotic flowers, grown from vanilla flowers with exotic pollen. Same properties as
     *        vanilla's small flowers.
     */
    public static final RegistryObject<Block> MENDING_LILAC = Registers.registerBlock(
        "mending_lilac",
        AuraFlowerBlock::mendingLilac,
        flowerProperties(),
        true
    );

    public static final RegistryObject<Block> MOONLIT_DANDELION = Registers.registerBlock(
        "moonlit_dandelion",
        AuraFlowerBlock::moonlitDandelion,
        flowerProperties(),
        true
    );

    public static final RegistryObject<Block> SHIMMER_LILY = Registers.registerBlock(
        "shimmer_lily",
        AuraFlowerBlock::shimmerLily,
        flowerProperties(),
        true
    );

    public static final RegistryObject<Block> FIREFLY_PETUNIA = Registers.registerBlock(
        "firefly_petunia",
        FireflyPetuniaBlock::new,
        flowerProperties().lightLevel(state -> 10),
        true
    );

    /**
     * @brief Glowing copies of each kind of vanilla leaves, made by using exotic pollen on them,
     *        keyed by the leaves they came from.
     *
     * No items: they drop and pick-block as the original leaves.
     *
     * @see GlowingLeavesBlock
     */
    public static final Map<Block, RegistryObject<Block>> GLOWING_LEAVES = registerGlowingLeaves(
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.CHERRY_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.PALE_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES,
            Blocks.AZALEA_LEAVES,
            Blocks.FLOWERING_AZALEA_LEAVES);

    private static Map<Block, RegistryObject<Block>> registerGlowingLeaves(Block... sources) {
        Map<Block, RegistryObject<Block>> blocks = new LinkedHashMap<>();
        for (Block source : sources) {
            String name = "glowing_" + BuiltInRegistries.BLOCK.getKey(source).getPath();
            blocks.put(source, Registers.registerBlock(name, properties -> new GlowingLeavesBlock(source, properties),
                GlowingLeavesBlock.propertiesOf(source), false));
        }
        return Collections.unmodifiableMap(blocks);
    }

    public static BlockBehaviour.Properties flowerProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .instabreak()
            .sound(SoundType.GRASS)
            .offsetType(BlockBehaviour.OffsetType.XZ)
            .pushReaction(PushReaction.DESTROY);
    }

    /** @brief Loading this class is what registers everything above. */
    static void init() {}

    private FlowerBlocks() {}
}
