package com.xincraft.xincraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

// A straight rail (like the powered rail, it can slope but not curve) that holds minecarts at a
// steady, reduced speed: faster carts are slowed to it and slower ones are pushed up to it, with no
// redstone needed. The block itself is plain; the speed handling lives in
// entity.TinRailMinecartBehavior, keyed off ModTags.HONEY_RAILS.
public class HoneyRailBlock extends BaseRailBlock {
    public static final MapCodec<HoneyRailBlock> CODEC = simpleCodec(HoneyRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;

    public HoneyRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        registerDefaultState(stateDefinition.any()
            .setValue(SHAPE, RailShape.NORTH_SOUTH)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<HoneyRailBlock> codec() {
        return CODEC;
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
        builder.add(SHAPE, WATERLOGGED);
    }
}
