package com.xincraft.xincraft.transport.train;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// A block a linking bar can be used on, whose block entity is a LinkedBlockEntity
public interface LinkableBlock {
    // Where the block entity is, for a block that spans more than one position
    default BlockPos linkPos(BlockState state, BlockPos clicked) {
        return clicked;
    }
}
