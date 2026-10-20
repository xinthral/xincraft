package com.xincraft.xincraft.block;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

// The tin turbine: burns fuel at furnace speed into Config.heatTurbineEnergyPerTick FE per tick
public class HeatTurbineBlockEntity extends TurbineBlockEntity {
    public static final int CAPACITY = 50_000;

    public HeatTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_TURBINE.get(), pos, state);
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
