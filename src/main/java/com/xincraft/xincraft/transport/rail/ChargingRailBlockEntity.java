package com.xincraft.xincraft.transport.rail;

import com.xincraft.xincraft.transport.TransportBlockEntities;
import java.util.ArrayList;
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
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jspecify.annotations.Nullable;

/**
 * @brief A charging rail's link to the power network, its only source.
 *
 * Carts on it draw through it from the network, up to its transfer rate (TRANSFER here; see
 * ElectrumChargingRailBlockEntity for the faster rail), and it keeps a small reserve. Generators
 * and cables can't put energy into it, but anything can draw energy out.
 *
 * @see transport.power.PowerNodeBlockEntity
 */
public class ChargingRailBlockEntity extends BlockEntity {
    public static final int CAPACITY = 500;
    /** @brief Most charge passed to each neighbouring rail per tick, by rails that pass charge. */
    private static final int PASS_RATE = 500;
    public static final int TRANSFER = 1_000;
    /** @brief How many chunks away, on each axis, it can draw from a power network. */
    public static final int CHUNK_RANGE = 2;
    /**
     * @brief How often it looks again for the charging stations and substations in range, in ticks.
     */
    private static final int NODE_SEARCH_TICKS = 40;
    private static final int SYNC_TICKS = 100;
    /**
     * @brief How often it decides whether it counts as charged, in ticks, so it doesn't flicker.
     */
    private static final int CHARGED_CHECK_TICKS = 10;

    private final int transfer;
    private final int chunkRange;
    private final Buffer buffer;
    private List<PowerNodeBlockEntity> nodesInRange = List.of();
    private long nextNodeSearch;
    private boolean energyChanged;
    private boolean charged;
    private boolean receivedSinceCheck;
    private int chargedCheckIn;
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
        this.energyCap = LazyOptional.of(Output::new);
    }

    void serverTick(Level level) {
        drawFromNetwork(level);
        passCharge(level);
        chargeCarts(level);
        syncEnergy(level);
        updateCharged(level);
    }

    /**
     * @brief Whether it's holding charge, or has been getting some: the rail is powered while it
     *        is.
     *
     * @return True if It's holding charge, or has been getting some.
     */
    public boolean isCharged() {
        return charged;
    }

    /**
     * @brief Holding nothing doesn't mean it's cut off, as a cart can be drawing straight through
     *        it from the network, so what it's received lately counts too.
     *
     * @param level The level to work in.
     */
    private void updateCharged(Level level) {
        if (--chargedCheckIn > 0) {
            return;
        }
        chargedCheckIn = CHARGED_CHECK_TICKS;
        boolean wasCharged = charged;
        charged = energy() > 0 || receivedSinceCheck;
        receivedSinceCheck = false;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof ChargingRailBlock rail) {
            rail.refreshPower(state, level, worldPosition);
            // Its redstone signal goes with its charge, whether or not its powered state changed
            if (charged != wasCharged) {
                level.updateNeighborsAt(worldPosition, rail);
            }
        }
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

    /**
     * @brief Lets clients know a changed amount, for rose-tinted glasses, at most once every 5
     *        seconds.
     *
     * @param level The level to work in.
     */
    private void syncEnergy(Level level) {
        long now = level.getGameTime();
        if (energyChanged && now >= nextSync) {
            energyChanged = false;
            nextSync = now + SYNC_TICKS;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * @brief Tops up from the network, as a small reserve.
     *
     * @param level The level to work in.
     */
    private void drawFromNetwork(Level level) {
        int space = buffer.receiveEnergy(transfer, true);
        if (space > 0) {
            buffer.receiveEnergy(drawFromNodes(level, space), false);
        }
    }

    /**
     * @brief Takes energy from the nearest charging station or substation in range, moving on to
     *        the next if that one's network has run dry, so overlapping stations cover for each
     *        other.
     *
     * @param level The level to work in.
     * @param amount The most to take.
     * @return How much it took.
     */
    private int drawFromNodes(Level level, int amount) {
        long now = level.getGameTime();
        if (now >= nextNodeSearch) {
            nodesInRange = PowerNodeBlockEntity.inRange(level, worldPosition, chunkRange);
            nextNodeSearch = now + NODE_SEARCH_TICKS;
        }
        int drawn = 0;
        for (PowerNodeBlockEntity node : nodesInRange) {
            if (drawn >= amount) {
                break;
            }
            if (!node.isRemoved()) {
                drawn += node.drawFromNetwork(amount - drawn);
            }
        }
        return drawn;
    }

    /**
     * @brief Evens out charge with the rails this one is joined to along the track, so a line of
     *        them carries power past the range of any node.
     *
     * Charge flows downhill, so the far end of a long line fills slowly.
     *
     * @param level The level to work in.
     */
    private void passCharge(Level level) {
        if (!passesCharge()) {
            return;
        }
        for (ChargingRailBlockEntity other : connectedRails(level)) {
            int amount = Math.min(PASS_RATE, (energy() - other.energy()) / 2);
            if (amount > 0) {
                buffer.extractEnergy(other.buffer.receiveEnergy(amount, false), false);
            }
        }
    }

    /**
     * @brief Whether this rail passes charge to others of its kind.
     *
     * @return True if This rail passes charge to others of its kind.
     */
    public boolean passesCharge() {
        return false;
    }

    /**
     * @brief Whether a rail it's joined to along the track has charge to pass along.
     *
     * @param level The level to work in.
     * @return True if A rail it's joined to along the track has charge to pass along.
     */
    public boolean hasChargedNeighbour(Level level) {
        return passesCharge() && connectedRails(level).stream().anyMatch(other -> other.energy() > 0);
    }

    /**
     * @brief The rails of its kind joined to this one along the track.
     *
     * This goes by each rail's shape, so two lines running side by side aren't joined. Shapes are
     * read from the level, as a block entity's own copy of its state can lag behind when a rail
     * changes shape.
     *
     * @param level The level to work in.
     * @return The joined rails.
     */
    private List<ChargingRailBlockEntity> connectedRails(Level level) {
        RailShape shape = shape(level.getBlockState(worldPosition));
        if (shape == null) {
            return List.of();
        }
        List<ChargingRailBlockEntity> found = new ArrayList<>(2);
        for (Direction exit : exits(shape)) {
            for (int rise = -1; rise <= 1; rise++) {
                BlockPos pos = worldPosition.relative(exit).above(rise);
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ChargingRailBlockEntity other && other.passesCharge()) {
                    RailShape otherShape = shape(level.getBlockState(pos));
                    if (otherShape != null && exits(otherShape).contains(exit.getOpposite()) && meets(shape, otherShape, exit, rise)) {
                        found.add(other);
                    }
                }
            }
        }
        return found;
    }

    private static @Nullable RailShape shape(BlockState state) {
        return state.getBlock() instanceof BaseRailBlock rail ? state.getValue(rail.getShapeProperty()) : null;
    }

    /**
     * @brief Which sides a rail of this shape leads out of.
     *
     * @param shape The rail's shape.
     * @return The sides.
     */
    private static List<Direction> exits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> List.of(Direction.NORTH, Direction.SOUTH);
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> List.of(Direction.EAST, Direction.WEST);
            case SOUTH_EAST -> List.of(Direction.SOUTH, Direction.EAST);
            case SOUTH_WEST -> List.of(Direction.SOUTH, Direction.WEST);
            case NORTH_WEST -> List.of(Direction.NORTH, Direction.WEST);
            case NORTH_EAST -> List.of(Direction.NORTH, Direction.EAST);
        };
    }

    /**
     * @brief Which way a sloped rail rises.
     *
     * @param shape The rail's shape.
     * @return The side it rises toward, or null if it's flat.
     */
    private static @Nullable Direction rises(RailShape shape) {
        return switch (shape) {
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            default -> null;
        };
    }

    /**
     * @brief Whether two rails rise blocks apart in height along exit actually meet.
     *
     * A step up needs this rail to slope up to it, a step down needs the other to slope up to this
     * one, and on the level neither may slope away over the join.
     *
     * @param shape This rail's shape.
     * @param otherShape The other rail's shape.
     * @param exit The side this rail leads out of.
     * @param rise How far above this rail the other one is.
     * @return True if Two rails rise blocks apart in height along exit actually meet.
     */
    private static boolean meets(RailShape shape, RailShape otherShape, Direction exit, int rise) {
        boolean climbs = rises(shape) == exit;
        boolean otherClimbs = rises(otherShape) == exit.getOpposite();
        return rise == 1 ? climbs : rise == -1 ? otherClimbs : !climbs && !otherClimbs;
    }

    /**
     * @brief Hands energy to any cart on the rail that takes it, up to the rail's transfer rate.
     *
     * From its reserve, and then straight from the network, so the small buffer doesn't hold
     * charging back.
     *
     * @param level The level to work in.
     */
    private void chargeCarts(Level level) {
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, new AABB(worldPosition))) {
            cart.getCapability(ForgeCapabilities.ENERGY, null).ifPresent(target -> {
                if (target.canReceive()) {
                    int wanted = target.receiveEnergy(transfer, true);
                    int held = buffer.extractEnergy(wanted, false);
                    int direct = wanted > held ? drawFromNodes(level, wanted - held) : 0;
                    int supplied = held + direct;
                    receivedSinceCheck |= direct > 0;
                    int taken = target.receiveEnergy(supplied, false);
                    if (taken < supplied) {
                        buffer.receiveEnergy(supplied - taken, false);
                    }
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
        energyCap = LazyOptional.of(Output::new);
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

    /**
     * @brief What generators, cables and machines see: energy can be drawn out, but none put in.
     */
    private class Output implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return buffer.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return buffer.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return buffer.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    /** @brief Marks the block entity for saving whenever its energy changes. */
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
                receivedSinceCheck = true;
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
