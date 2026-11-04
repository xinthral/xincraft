package com.xincraft.xincraft.turbines;

import com.xincraft.xincraft.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @brief The tin turbine: burns fuel at furnace speed into Config.heatTurbineEnergyPerTick FE per
 *        tick.
 */
public class HeatTurbineBlockEntity extends TurbineBlockEntity {
    public static final int CAPACITY = 50_000;

    public HeatTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineBlockEntities.HEAT_TURBINE.get(), pos, state);
    }

    @Override
    protected int energyCapacity() {
        return CAPACITY;
    }

    @Override
    protected int energyPerTick() {
        return Config.heatTurbineEnergyPerTick;
    }
}
