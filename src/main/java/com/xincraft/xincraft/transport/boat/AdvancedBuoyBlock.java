package com.xincraft.xincraft.transport.boat;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * @brief A buoy whose links reach ADVANCED_LINK_RANGE blocks, but only to another advanced buoy.
 *
 * Linked to an ordinary buoy, it reaches as far as that one does.
 *
 * Motorboats need the advanced antenna upgrade to follow a leg longer than an ordinary buoy's.
 *
 * @see BuoyPath
 */
public class AdvancedBuoyBlock extends BuoyBlock {
    public static final MapCodec<AdvancedBuoyBlock> CODEC = simpleCodec(AdvancedBuoyBlock::new);

    public AdvancedBuoyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AdvancedBuoyBlock> codec() {
        return CODEC;
    }

    @Override
    public double linkRange() {
        return BuoyBlockEntity.ADVANCED_LINK_RANGE;
    }
}
