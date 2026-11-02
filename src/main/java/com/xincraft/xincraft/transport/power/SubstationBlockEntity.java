package com.xincraft.xincraft.transport.power;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// Stores a little of a power network's energy, passed to it by the charging stations on the network,
// and extends the network's reach. Energy only reaches it through the network, so it has no energy
// connection of its own.
public class SubstationBlockEntity extends PowerNodeBlockEntity {
    public static final int CAPACITY = 5_000;

    public SubstationBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.SUBSTATION.get(), pos, state, CAPACITY);
    }
}
