package com.xincraft.xincraft.transport.rail;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * @brief A bronze powered rail on a power network, passing energy to a cart on it that takes
 *        energy, such as a locomotive cart with the energy upgrade.
 *
 * Otherwise it's a powered rail, powered by charge as well as by redstone: with charge it boosts
 * carts on their way, and without either it brakes and holds them. With charge it also gives off a
 * full redstone signal.
 *
 * @see ChargingRailBlockEntity
 */
public class ChargingRailBlock extends ModPoweredRailBlock implements EntityBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(ChargingRailBlock::new);
    private static final int FULL_SIGNAL = 15;

    public ChargingRailBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * @brief As for any powered rail, plus charge, which doesn't pass along the line as redstone
     *        power does.
     */
    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        boolean charged = level.getBlockEntity(pos) instanceof ChargingRailBlockEntity rail && rail.isCharged();
        boolean shouldPower = charged || level.hasNeighborSignal(pos)
            || findPoweredRailSignal(level, pos, state, true, 0)
            || findPoweredRailSignal(level, pos, state, false, 0);
        if (shouldPower != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, shouldPower), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos.below(), this);
            if (state.getValue(getShapeProperty()).isSlope()) {
                level.updateNeighborsAt(pos.above(), this);
            }
        }
    }

    void refreshPower(BlockState state, Level level, BlockPos pos) {
        updateState(state, level, pos, this);
    }

    /**
     * @brief A full signal comes from charge alone, not from being powered.
     *
     * Rails beside each other would otherwise keep each other powered forever once started.
     */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ChargingRailBlockEntity rail && rail.isCharged() ? FULL_SIGNAL : 0;
    }

    @Override
    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingRailBlockEntity(pos, state);
    }

    protected BlockEntityType<? extends ChargingRailBlockEntity> blockEntityType() {
        return TransportBlockEntities.CHARGING_RAIL.get();
    }

    /** @brief Only the server moves energy. */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != blockEntityType()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ChargingRailBlockEntity>) (tickLevel, pos, tickState, rail) -> rail.serverTick(tickLevel);
    }
}
