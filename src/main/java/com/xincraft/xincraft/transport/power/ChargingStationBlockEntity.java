package com.xincraft.xincraft.transport.power;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.xincraft.xincraft.transport.rail.ChargingRailBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

// Where energy enters a power network. Each tick it draws from the energy blocks around it (turbines
// also push into it, as can cables), then passes what it holds on to the substations on its network.
// Energy only goes in: nothing can draw it back out but charging rails in range.
public class ChargingStationBlockEntity extends PowerNodeBlockEntity {
    public static final int CAPACITY = 2_000;
    public static final int TRANSFER = 1_000;

    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(Input::new).cast();

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.CHARGING_STATION.get(), pos, state, CAPACITY);
    }

    @Override
    protected void serverTick(Level level) {
        super.serverTick(level);
        drawFromNeighbours(level);
        for (PowerNodeBlockEntity node : network()) {
            if (energy() <= 0) {
                return;
            }
            if (node instanceof SubstationBlockEntity && !node.isRemoved()) {
                drain(node.fill(energy()));
            }
        }
    }

    // Pulls from whatever's around that gives energy out, for sources that don't push it in
    // themselves. Charging rails give energy out too, but it's the network that feeds them.
    private void drawFromNeighbours(Level level) {
        for (Direction direction : Direction.values()) {
            int space = Math.min(TRANSFER, capacity() - energy());
            if (space <= 0) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null || neighbour instanceof ChargingRailBlockEntity || neighbour instanceof PowerNodeBlockEntity) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(source -> {
                if (source.canExtract()) {
                    fill(source.extractEnergy(space, false));
                }
            });
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(Input::new).cast();
    }

    // What cables and generators see: energy goes in, up to TRANSFER at a time
    private class Input implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int amount = Math.max(0, Math.min(Math.min(maxReceive, TRANSFER), capacity() - energy()));
            return simulate ? amount : fill(amount);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy();
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
