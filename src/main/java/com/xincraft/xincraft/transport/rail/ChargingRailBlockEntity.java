package com.xincraft.xincraft.transport.rail;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import java.util.List;
import com.xincraft.xincraft.transport.power.PowerNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
// cables on every side, taking energy in and letting anything draw it out, up to its transfer rate
// per tick each way (TRANSFER here; see ElectrumChargingRailBlockEntity for the faster rail). It also
// draws from the nearest power network in range (see transport.power.PowerNodeBlockEntity).
public class ChargingRailBlockEntity extends BlockEntity {
    public static final int CAPACITY = 5_000;
    public static final int TRANSFER = 1_000;
    // How many chunks away, on each axis, it can draw from a power network
    public static final int CHUNK_RANGE = 2;
    // How often it looks again for the charging stations and substations in range, in ticks
    private static final int NODE_SEARCH_TICKS = 40;
    private static final int SYNC_TICKS = 100;

    private final int transfer;
    private final int chunkRange;
    private final Buffer buffer;
    private List<PowerNodeBlockEntity> nodesInRange = List.of();
    private long nextNodeSearch;
    private boolean energyChanged;
    private long nextSync;
    private LazyOptional<IEnergyStorage> energyCap;

    public ChargingRailBlockEntity(BlockPos pos, BlockState state) {
        this(TransportBlockEntities.CHARGING_RAIL.get(), pos, state, CAPACITY, TRANSFER, CHUNK_RANGE);
    }

    protected ChargingRailBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int transfer, int chunkRange) {
        super(type, pos, state);
        this.transfer = transfer;
        this.chunkRange = chunkRange;
        this.buffer = new Buffer(capacity);
        this.energyCap = LazyOptional.of(() -> buffer);
    }

    void serverTick(Level level) {
        drawFromNeighbours(level);
        drawFromNetwork(level);
        chargeCarts(level);
        syncEnergy(level);
    }

    public int energy() {
        return buffer.getEnergyStored();
    }

    public int capacity() {
        return buffer.getMaxEnergyStored();
    }

    public int chunkRange() {
        return chunkRange;
    }

    // Lets clients know a changed amount, for rose-tinted glasses, at most once every 5 seconds
    private void syncEnergy(Level level) {
        long now = level.getGameTime();
        if (energyChanged && now >= nextSync) {
            energyChanged = false;
            nextSync = now + SYNC_TICKS;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Tops up from the nearest charging station or substation in range, moving on to the next if
    // that one's network has run dry, so overlapping stations cover for each other
    private void drawFromNetwork(Level level) {
        int space = buffer.receiveEnergy(transfer, true);
        if (space <= 0) {
            return;
        }
        long now = level.getGameTime();
        if (now >= nextNodeSearch) {
            nodesInRange = PowerNodeBlockEntity.inRange(level, worldPosition, chunkRange);
            nextNodeSearch = now + NODE_SEARCH_TICKS;
        }
        for (PowerNodeBlockEntity node : nodesInRange) {
            if (space <= 0) {
                return;
            }
            if (!node.isRemoved()) {
                int drawn = node.drawFromNetwork(space);
                buffer.receiveEnergy(drawn, false);
                space -= drawn;
            }
        }
    }

    // Pulls from whatever's around the rail that gives energy out, for sources that don't push it
    // in themselves. Other charging rails are skipped, so a line of them doesn't pass energy back
    // and forth.
    private void drawFromNeighbours(Level level) {
        for (Direction direction : Direction.values()) {
            int space = buffer.receiveEnergy(transfer, true);
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
                    int offered = buffer.extractEnergy(transfer, true);
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
        Buffer(int capacity) {
            super(capacity, transfer, transfer);
        }

        void set(int amount) {
            energy = Math.clamp(amount, 0, capacity);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                energyChanged = true;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                energyChanged = true;
                setChanged();
            }
            return extracted;
        }
    }
}
