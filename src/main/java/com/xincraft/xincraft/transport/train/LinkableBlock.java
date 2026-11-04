package com.xincraft.xincraft.transport.train;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** @brief A block a linking bar can be used on, whose block entity is a LinkedBlockEntity. */
public interface LinkableBlock {
    /**
     * @brief Where the block entity is, for a block that spans more than one position.
     *
     * @param state The block's state.
     * @param clicked The position the bar was used on.
     * @return The block entity's position.
     */
    default BlockPos linkPos(BlockState state, BlockPos clicked) {
        return clicked;
    }
}
