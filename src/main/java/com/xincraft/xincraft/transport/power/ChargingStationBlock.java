package com.xincraft.xincraft.transport.power;

import com.mojang.serialization.MapCodec;
import com.xincraft.xincraft.transport.TransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// Feeds a power network from the energy blocks around it (see ChargingStationBlockEntity)
public class ChargingStationBlock extends PowerNodeBlock {
    public static final MapCodec<ChargingStationBlock> CODEC = simpleCodec(ChargingStationBlock::new);

    public ChargingStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ChargingStationBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    // Only the server moves energy
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TransportBlockEntities.CHARGING_STATION.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<ChargingStationBlockEntity>) (tickLevel, pos, tickState, station) -> station.serverTick(tickLevel);
    }
}
