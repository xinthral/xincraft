package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Leaves dusted with exotic pollen (item.ExoticPollenItem): a copy of one kind of vanilla leaves that
// gives off a faint light, like glow lichen. Everything else comes from the original leaves: its
// look and tint (models and colours point at the original's), name, drops, decay, sounds and falling
// leaf particles. Pick-block gives the original leaves.
public class GlowingLeavesBlock extends LeavesBlock {
    public static final int LIGHT = 7;
    public static final MapCodec<GlowingLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("source").forGetter(GlowingLeavesBlock::getSource),
        propertiesCodec()
    ).apply(i, GlowingLeavesBlock::new));

    private final Block source;

    public GlowingLeavesBlock(Block source, BlockBehaviour.Properties properties) {
        // No falling-leaf chance of its own: animateTick hands that to the original leaves
        super(0.0F, properties);
        this.source = source;
    }

    // The original leaves' properties, with the light added and its name and drops kept
    public static BlockBehaviour.Properties propertiesOf(Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source)
            .lightLevel(state -> LIGHT)
            .overrideDescription(source.getDescriptionId())
            .overrideLootTable(source.getLootTable());
    }

    public Block getSource() {
        return source;
    }

    @Override
    public MapCodec<GlowingLeavesBlock> codec() {
        return CODEC;
    }

    // Rain drips and falling leaves, exactly as the original leaves make them
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        source.animateTick(source.withPropertiesOf(state), level, pos, random);
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {}

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(source);
    }
}
