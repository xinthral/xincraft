package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

// A straight rail that launches minecarts into the air, higher the stronger the redstone signal
// it receives (0 = an ordinary rail). The block only tracks that signal strength in POWER; the
// launch itself lives in entity.TinRailMinecartBehavior, keyed off ModTags.BOUNCY_RAILS.
public class BouncyRailBlock extends BaseRailBlock {
    public static final MapCodec<BouncyRailBlock> CODEC = simpleCodec(BouncyRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public BouncyRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        registerDefaultState(stateDefinition.any()
            .setValue(SHAPE, RailShape.NORTH_SOUTH)
            .setValue(POWER, 0)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<BouncyRailBlock> codec() {
        return CODEC;
    }

    // Called by BaseRailBlock when the rail is placed and whenever a neighbour changes. Unlike a
    // powered rail, power doesn't pass along a line of these; each one reads its own signal.
    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        int signal = level.getBestNeighborSignal(pos);
        if (signal != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, signal), Block.UPDATE_ALL);
        }
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWER, WATERLOGGED);
    }
}
