package com.xincraft.xincraft.transport.rail;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// The electrum charging rail: four times as fast as the charging rail, able to draw from a power
// network twice as far away, and passing charge along a line of its kind
public class ElectrumChargingRailBlockEntity extends ChargingRailBlockEntity {
    public static final int CAPACITY = 500;
    public static final int TRANSFER = 4_000;
    public static final int CHUNK_RANGE = 4;

    public ElectrumChargingRailBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.ELECTRUM_CHARGING_RAIL.get(), pos, state, CAPACITY, TRANSFER, CHUNK_RANGE);
    }

    @Override
    public boolean passesCharge() {
        return true;
    }
}
