package com.xincraft.xincraft.block;

import com.xincraft.xincraft.Config;
import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jspecify.annotations.Nullable;

// The bronze turbine: boils water into steam. It burns fuel twice as fast as the heat turbine but
// makes Config.steamTurbineEnergyPerTick FE per tick, so each fuel item yields more energy overall.
// It uses Config.steamTurbineWaterPerTick mB of water per tick from its tank, filled with water
// buckets or by other mods' pipes from any side, and stops (without using fuel) when the tank's dry.
public class SteamTurbineBlockEntity extends TurbineBlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int TANK_CAPACITY = 8_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE.get(), pos, state);
    }

    @Override
    protected int energyCapacity() {
        return CAPACITY;
    }

    @Override
    protected int energyPerTick() {
        return Config.steamTurbineEnergyPerTick;
    }

    @Override
    protected int burnSpeed() {
        return 2;
    }

    @Override
    protected boolean canRun() {
        return tank.getFluidAmount() >= Config.steamTurbineWaterPerTick;
    }

    @Override
    protected void onRun() {
        tank.drain(Config.steamTurbineWaterPerTick, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected int waterAmount() {
        return tank.getFluidAmount();
    }

    @Override
    protected int waterCapacity() {
        return TANK_CAPACITY;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCap.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(() -> tank);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.readFrom(input.childOrEmpty("Tank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.writeTo(output.child("Tank"));
    }
}
