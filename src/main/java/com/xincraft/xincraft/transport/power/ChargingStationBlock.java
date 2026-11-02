package com.xincraft.xincraft.transport.power;

import java.util.Map;
import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.transport.TransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Feeds a power network from the turbines and other energy blocks connected to it with a linking bar
// (see ChargingStationBlockEntity). An arm reaches out towards each one, as a fence does to its neighbours.
public class ChargingStationBlock extends PowerNodeBlock {
    public static final MapCodec<ChargingStationBlock> CODEC = simpleCodec(ChargingStationBlock::new);
    // Which sides have a power source connected
    public static final Map<Direction, BooleanProperty> CONNECTED = Map.of(
        Direction.NORTH, BlockStateProperties.NORTH,
        Direction.EAST, BlockStateProperties.EAST,
        Direction.SOUTH, BlockStateProperties.SOUTH,
        Direction.WEST, BlockStateProperties.WEST);

    public ChargingStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTED.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    protected MapCodec<ChargingStationBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends PowerNodeBlockEntity> blockEntityType() {
        return TransportBlockEntities.CHARGING_STATION.get();
    }
}
