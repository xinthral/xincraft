package com.xincraft.xincraft.block;

import com.xincraft.xincraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

// The charging rail's energy buffer. Each tick it draws energy from the energy blocks around it
// (generators, batteries and the like) and hands energy to the carts on it. It's also open to
// cables on every side, taking energy in and letting anything draw it out, up to TRANSFER per
// tick each way.
public class ChargingRailBlockEntity extends BlockEntity {
    public static final int CAPACITY = 50_000;
    public static final int TRANSFER = 1_000;

    private final Buffer buffer = new Buffer();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> buffer);

    public ChargingRailBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_RAIL.get(), pos, state);
    }

    void serverTick(Level level) {
        drawFromNeighbours(level);
        chargeCarts(level);
    }

    // Pulls from whatever's around the rail that gives energy out, for sources that don't push it
    // in themselves. Other charging rails are skipped, so a line of them doesn't pass energy back
    // and forth.
    private void drawFromNeighbours(Level level) {
        for (Direction direction : Direction.values()) {
            int space = buffer.receiveEnergy(TRANSFER, true);
            if (space <= 0) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbour == null || neighbour instanceof ChargingRailBlockEntity) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(source -> {
                if (source.canExtract()) {
                    buffer.receiveEnergy(source.extractEnergy(space, false), false);
                }
            });
        }
    }

    // Hands energy to any cart on the rail that takes it
    private void chargeCarts(Level level) {
        if (buffer.getEnergyStored() <= 0) {
            return;
        }
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(worldPosition))) {
            cart.getCapability(ForgeCapabilities.ENERGY, null).ifPresent(target -> {
                if (target.canReceive()) {
                    int offered = buffer.extractEnergy(TRANSFER, true);
                    buffer.extractEnergy(target.receiveEnergy(offered, false), false);
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
        energyCap = LazyOptional.of(() -> buffer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.set(input.getIntOr("Energy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", buffer.getEnergyStored());
    }

    // Marks the block entity for saving whenever its energy changes
    private class Buffer extends EnergyStorage {
        Buffer() {
            super(CAPACITY, TRANSFER, TRANSFER);
        }

        void set(int amount) {
            energy = Math.clamp(amount, 0, capacity);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    }
}
