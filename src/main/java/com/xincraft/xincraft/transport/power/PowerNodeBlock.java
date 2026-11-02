package com.xincraft.xincraft.transport.power;

import com.xincraft.xincraft.transport.train.LinkableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A charging station or substation: a squat cabinet with a little antenna on top, which is left out
// of its shape so it doesn't snag anything. See PowerNodeBlockEntity.
public abstract class PowerNodeBlock extends BaseEntityBlock implements LinkableBlock {
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 11.0);

    protected PowerNodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
