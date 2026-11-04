package com.xincraft.xincraft.transport.power;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import com.xincraft.xincraft.transport.TransportItems;
import com.xincraft.xincraft.transport.rail.ChargingRailBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * @brief Where energy enters a power network.
 *
 * A turbine or other energy block right beside it feeds it once connected with a linking bar. Each
 * tick it draws from those, then passes what it holds on to the substations on its network. Energy
 * only goes in: nothing can draw it back out but charging rails in range.
 *
 * @see transport.train.LinkingBarItem
 */
public class ChargingStationBlockEntity extends PowerNodeBlockEntity {
    public static final int CAPACITY = 2_000;
    public static final int TRANSFER = 1_000;

    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(Input::new).cast();
    private boolean removing;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(TransportBlockEntities.CHARGING_STATION.get(), pos, state, CAPACITY);
    }

    @Override
    protected void serverTick(Level level) {
        super.serverTick(level);
        dropMissingSources(level);
        drawFromSources(level);
        for (PowerNodeBlockEntity node : network()) {
            if (energy() <= 0) {
                return;
            }
            if (node instanceof SubstationBlockEntity && !node.isRemoved()) {
                drain(node.fill(energy()));
            }
        }
    }

    public boolean isConnected(Direction side) {
        BooleanProperty property = ChargingStationBlock.CONNECTED.get(side);
        // From the level, as a block entity's own copy of its state can lag behind
        BlockState state = level != null ? level.getBlockState(worldPosition) : getBlockState();
        return property != null && state.hasProperty(property) && state.getValue(property);
    }

    public int sourceCount() {
        return (int) ChargingStationBlock.CONNECTED.keySet().stream().filter(this::isConnected).count();
    }

    @Override
    public boolean hasSource() {
        return sourceCount() > 0;
    }

    @Override
    public boolean connectsSources() {
        return true;
    }

    /**
     * @brief Whether the block at pos could be connected to a station: it has energy, and isn't
     *        part of the network itself.
     *
     * @param level The level to work in.
     * @param pos The block's position.
     * @param side The side of it facing the station.
     * @return True if The block at pos could be connected to a station.
     */
    public static boolean isSource(Level level, BlockPos pos, Direction side) {
        BlockEntity block = level.getBlockEntity(pos);
        return block != null && !(block instanceof ChargingRailBlockEntity) && !(block instanceof PowerNodeBlockEntity)
            && block.getCapability(ForgeCapabilities.ENERGY, side).isPresent();
    }

    /**
     * @brief Connects the energy block on one side.
     *
     * The caller has checked there is one, and that the side is free.
     *
     * @param side The side it's on.
     */
    public void connect(Direction side) {
        setConnected(side, true);
    }

    private void setConnected(Direction side, boolean connected) {
        if (level != null) {
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(ChargingStationBlock.CONNECTED.get(side), connected), Block.UPDATE_ALL);
        }
    }

    /**
     * @brief An energy block that's gone takes its connection with it, leaving the bar behind.
     *
     * Sides in unloaded chunks are left alone, as what's there can't be checked.
     *
     * @param level The level to work in.
     */
    private void dropMissingSources(Level level) {
        for (Direction side : ChargingStationBlock.CONNECTED.keySet()) {
            BlockPos neighbour = worldPosition.relative(side);
            if (isConnected(side) && level.isLoaded(neighbour) && !isSource(level, neighbour, side.getOpposite())) {
                setConnected(side, false);
                Block.popResource(level, worldPosition, new ItemStack(TransportItems.LINKING_BAR.get()));
            }
        }
    }

    /**
     * @brief Pulls from connected sources that don't push energy in themselves.
     *
     * @param level The level to work in.
     */
    private void drawFromSources(Level level) {
        for (Direction side : ChargingStationBlock.CONNECTED.keySet()) {
            int space = Math.min(TRANSFER, capacity() - energy());
            if (space <= 0) {
                return;
            }
            if (!isConnected(side)) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(side));
            if (neighbour != null) {
                neighbour.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).ifPresent(source -> {
                    if (source.canExtract()) {
                        fill(source.extractEnergy(space, false));
                    }
                });
            }
        }
    }

    /**
     * @brief Unlinking also lets go of its sources, each of which took a bar.
     *
     * While the block is being removed, its state is left alone.
     */
    @Override
    public int unlinkAll() {
        int count = super.unlinkAll();
        for (Direction side : ChargingStationBlock.CONNECTED.keySet()) {
            if (isConnected(side)) {
                count++;
                if (!removing) {
                    setConnected(side, false);
                }
            }
        }
        return count;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        removing = true;
        super.preRemoveSideEffects(pos, state);
    }

    /**
     * @brief Only a connected side takes energy, so a turbine beside the station feeds it only once
     *        linked.
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return side != null && isConnected(side) ? energyCap.cast() : LazyOptional.empty();
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

    /** @brief What cables and generators see: energy goes in, up to TRANSFER at a time. */
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
